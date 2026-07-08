#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Codelist Extractor - Comprehensive codelist extraction from DB + XPT files.

Sources:
  1. Variables level: sas_project_spec.cdisc_submission_value → read XPT column to get Terms.
  2. VLM level: sas_vlm_data rows → filter XPT by Where Clause, read --STRESC unique values.

Handles deduplication and removes empty values.
"""

import os
import re
import sys
import pymysql
import pyreadstat
import pandas as pd
from pathlib import Path
from typing import Dict, List, Optional, Tuple

sys.path.append(str(Path(__file__).parent))


def _get_db_config() -> Dict[str, str]:
    return {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'user': os.environ.get('DB_USER', 'root'),
        'password': os.environ.get('DB_PASSWORD', '123123'),
        'database': os.environ.get('DB_NAME', 'define_db'),
    }


def find_xpt_file(data_path: Path, dataset: str) -> Optional[Path]:
    direct = data_path / f"{dataset}.xpt"
    if direct.exists():
        return direct
    for variant in [dataset.upper(), dataset.lower()]:
        p = data_path / f"{variant}.xpt"
        if p.exists():
            return p
    matches = list(data_path.glob(f"*_{dataset}.xpt")) + list(data_path.glob(f"*_{dataset.lower()}.xpt"))
    if matches:
        return matches[0]
    for p in data_path.glob("*.xpt"):
        stem = p.stem.lower()
        if stem == dataset.lower() or stem.endswith(f"_{dataset.lower()}"):
            return p
    return None


def read_xpt(xpt_path: Path) -> pd.DataFrame:
    df, _ = pyreadstat.read_xport(str(xpt_path))
    df.columns = df.columns.str.upper()
    for col in df.columns:
        if df[col].dtype == object:
            df[col] = df[col].apply(_clean_text)
    return df


def _clean_text(val):
    if pd.isna(val) or val is None:
        return ''
    s = str(val)
    if s == 'nan':
        return ''
    if s.startswith("b'") and s.endswith("'"):
        s = s[2:-1]
    try:
        s = s.encode('latin1').decode('utf-8')
    except Exception:
        pass
    return s.strip()


def parse_where_clause(wc: str) -> Optional[Tuple[str, str]]:
    if not wc or not wc.strip():
        return None
    m = re.match(r'(\w+)\s+EQ\s+"([^"]*)"', wc.strip())
    if m:
        return m.group(1).upper(), m.group(2)
    return None


def _is_numeric(s: str) -> bool:
    try:
        float(s)
        return True
    except (ValueError, TypeError):
        return False


def _infer_data_type(values: List[str]) -> str:
    if not values:
        return 'text'
    all_numeric = all(_is_numeric(v) for v in values if v)
    if all_numeric:
        has_decimal = any('.' in v for v in values if v)
        return 'float' if has_decimal else 'integer'
    return 'text'


def _term_weight(term: str) -> int:
    """Sort weight for a codelist term (lower = earlier)."""
    if not term:
        return 50
    t = term.strip()
    tu = t.upper()

    # Unknown / Other → always last
    if t in ('未知',) or tu in ('UNKNOWN', 'OTHER') or tu.startswith('UNKNOWN') or tu.startswith('OTHER') or t == '其他':
        return 90

    # Not done / not examined → after all regular terms
    if t in ('未查', '未做') or tu in ('NOT DONE', 'NOT EXAMINED') or tu.startswith('NOT DONE') or tu.startswith('NOT EXAMINED'):
        return 80

    # Clinical normalcy
    if t == '正常' or tu in ('NORMAL', 'WNL', 'WITHIN NORMAL LIMITS'):
        return 10
    if '无临床意义' in t or 'NCS' in tu or ('NOT CLINICALLY' in tu and 'SIGNIFICANT' in tu):
        return 20
    if t == '异常' or tu == 'ABNORMAL':
        return 25
    if '有临床意义' in t or ('CLINICALLY SIGNIFICANT' in tu and 'NOT' not in tu):
        return 30

    # Negative before positive
    if t == '阴性' or tu in ('NEGATIVE', 'NEG'):
        return 11
    if t == '阳性' or tu in ('POSITIVE', 'POS'):
        return 12

    return 50


def _sort_terms(terms: list) -> list:
    """Sort a list of {'code': ..., 'codeDes': ...} dicts."""
    if len(terms) <= 1:
        return terms
    return sorted(terms, key=lambda t: (_term_weight(t.get('code', '')), t.get('code', '').upper()))


def run(project_id: str, data_path: str, username: str = ''):
    db_config = _get_db_config()
    data_dir = Path(data_path)
    if not data_dir.exists():
        print(f"[ERROR] XPT path not found: {data_dir}")
        return False

    conn = pymysql.connect(
        host=db_config['host'], user=db_config['user'],
        password=db_config['password'], database=db_config['database'],
        charset='utf8mb4', autocommit=False,
    )

    try:
        # ── Load CT codelist_name lookup ─────────────────────────────────
        # Maps cdisc_submission_value → codelist_name from CT header rows
        ct_name_lookup: Dict[str, str] = {}
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            cur.execute(
                "SELECT ct_version FROM project_config WHERE project_id = %s LIMIT 1",
                (project_id,))
            cfg_row = cur.fetchone()
            ct_version_str = (cfg_row or {}).get('ct_version', '') or ''

            # Extract date part from ct_version (e.g. "2023-06-30")
            import re as _re
            date_match = _re.search(r'(\d{4}-\d{2}-\d{2})', ct_version_str)
            ct_date = date_match.group(1) if date_match else ''

            pkg_id = None
            if ct_date:
                cur.execute(
                    "SELECT id FROM ct_package WHERE release_date = %s LIMIT 1",
                    (ct_date,))
                pkg_row = cur.fetchone()
                if pkg_row:
                    pkg_id = pkg_row['id']

            if pkg_id is None:
                cur.execute(
                    "SELECT id FROM ct_package WHERE standard_type = 'SDTM' AND language_code = 'EN' "
                    "ORDER BY release_date DESC LIMIT 1")
                pkg_row = cur.fetchone()
                if pkg_row:
                    pkg_id = pkg_row['id']

            if pkg_id:
                cur.execute(
                    "SELECT codelist_name, cdisc_submission_value FROM ct_term "
                    "WHERE package_id = %s AND (codelist_code IS NULL OR codelist_code = '')",
                    (pkg_id,))
                for row in cur.fetchall():
                    sv = (row.get('cdisc_submission_value') or '').strip()
                    cn = (row.get('codelist_name') or '').strip()
                    if sv and cn:
                        ct_name_lookup[sv.upper()] = cn
                print(f"  CT lookup loaded: {len(ct_name_lookup)} codelist names from package {pkg_id}")
            else:
                print("  [WARN] No CT package found, will use spec labels for Name")

        # ── Load XPT files ───────────────────────────────────────────────
        xpt_cache: Dict[str, pd.DataFrame] = {}

        def get_xpt(ds: str) -> Optional[pd.DataFrame]:
            ds_upper = ds.upper()
            if ds_upper in xpt_cache:
                return xpt_cache[ds_upper]
            xpt_file = find_xpt_file(data_dir, ds)
            if not xpt_file:
                return None
            try:
                df = read_xpt(xpt_file)
                xpt_cache[ds_upper] = df
                print(f"  [OK] {ds_upper}.xpt loaded ({len(df)} rows)")
                return df
            except Exception as e:
                print(f"  [ERR] {ds_upper}.xpt: {e}")
                return None

        # ── Collect codelist definitions ─────────────────────────────────
        # Each codelist entry: {vcd, vlabel, type, terms: [{code, codeDes}]}
        codelist_map: Dict[str, dict] = {}

        def add_codelist(vcd: str, vlabel: str, terms: List[dict],
                         data_type: str = 'Char', subm_val: str = ''):
            """Add or merge terms into a codelist, deduplicating by code value."""
            vcd = vcd.strip()
            if not vcd:
                return
            # Use submission value (if provided) to look up CT English name
            lookup_key = subm_val.upper() if subm_val else vcd.upper()
            ct_name = ct_name_lookup.get(lookup_key, '')
            effective_label = ct_name if ct_name else (vlabel.strip() if vlabel else '')
            if vcd not in codelist_map:
                codelist_map[vcd] = {
                    'vcd': vcd,
                    'vlabel': effective_label,
                    'type': data_type,
                    'terms': [],
                    'seen_codes': set(),
                    'subm_val': subm_val or vcd,
                }
            entry = codelist_map[vcd]
            if ct_name:
                entry['vlabel'] = ct_name
            elif not entry['vlabel'] and vlabel:
                entry['vlabel'] = vlabel.strip()
            for t in terms:
                code = str(t.get('code', '')).strip()
                if not code:
                    continue
                if code in entry['seen_codes']:
                    continue
                entry['seen_codes'].add(code)
                entry['terms'].append({
                    'code': code,
                    'codeDes': str(t.get('codeDes', '')).strip(),
                })

        # ── Part 1: Variables-level codelists ────────────────────────────
        print("\n=== Part 1: Variables-level codelists ===")
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            sql = ("SELECT domain, variable, cdisc_submission_value, codelist, label "
                   "FROM sas_project_spec WHERE project_id = %s")
            params = [project_id]
            if username:
                sql += " AND username = %s"
                params.append(username)
            sql += " ORDER BY domain, variable"
            cur.execute(sql, params)
            spec_rows = cur.fetchall()

        general_vars: List[dict] = []
        domain_all_values: list = []   # collect all DOMAIN values across datasets
        domain_label: str = ''
        testcd_test_pairs: Dict[str, dict] = {}
        dict_ids_seen: set = set()  # track MEDDRA / WHODRUG already collected

        for row in spec_rows:
            subm_val = (row.get('cdisc_submission_value') or '').strip()
            if not subm_val:
                continue
            domain = (row.get('domain') or '').strip().upper()
            variable = (row.get('variable') or '').strip().upper()
            label = (row.get('label') or '').strip()

            # MEDDRA / WHODRUG → dictionaries, skip codelist extraction
            if subm_val.upper() in ('MEDDRA', 'WHODRUG'):
                dict_ids_seen.add(subm_val.upper())
                continue

            # DOMAIN: collect all values across all datasets into one merged codelist
            if subm_val.upper() == 'DOMAIN':
                df = get_xpt(domain)
                if df is not None and variable in df.columns:
                    vals = df[variable].dropna().astype(str).str.strip()
                    domain_all_values.extend(v for v in vals if v and v != 'nan')
                if not domain_label and label:
                    domain_label = label
                continue

            if subm_val.upper().endswith('TESTCD'):
                prefix = subm_val.upper().replace('TESTCD', '')
                testcd_test_pairs.setdefault(prefix, {})
                testcd_test_pairs[prefix]['testcd_var'] = variable
                testcd_test_pairs[prefix]['testcd_subm'] = subm_val
                testcd_test_pairs[prefix]['domain'] = domain
                testcd_test_pairs[prefix]['testcd_label'] = label
                continue
            if subm_val.upper().endswith('TEST') and not subm_val.upper().endswith('IETEST'):
                prefix = subm_val.upper().replace('TEST', '')
                testcd_test_pairs.setdefault(prefix, {})
                testcd_test_pairs[prefix]['test_var'] = variable
                testcd_test_pairs[prefix]['test_subm'] = subm_val
                testcd_test_pairs[prefix]['domain'] = domain
                testcd_test_pairs[prefix]['test_label'] = label
                continue

            if subm_val.upper().endswith('PARMCD'):
                prefix = subm_val.upper()[:-6]  # remove 'PARMCD'
                testcd_test_pairs.setdefault(prefix, {})
                testcd_test_pairs[prefix]['testcd_var'] = variable
                testcd_test_pairs[prefix]['testcd_subm'] = subm_val
                testcd_test_pairs[prefix]['domain'] = domain
                testcd_test_pairs[prefix]['testcd_label'] = label
                continue
            if subm_val.upper().endswith('PARM') and not subm_val.upper().endswith('PARMCD') and len(subm_val) > 4:
                prefix = subm_val.upper()[:-4]  # remove 'PARM'
                testcd_test_pairs.setdefault(prefix, {})
                testcd_test_pairs[prefix]['test_var'] = variable
                testcd_test_pairs[prefix]['test_subm'] = subm_val
                testcd_test_pairs[prefix]['domain'] = domain
                testcd_test_pairs[prefix]['test_label'] = label
                continue

            general_vars.append({
                'domain': domain,
                'variable': variable,
                'subm_val': subm_val,
                'label': label,
            })

        # 1a. DOMAIN merged codelist
        if domain_all_values:
            unique_domains = sorted(set(domain_all_values))
            terms = _sort_terms([{'code': v, 'codeDes': ''} for v in unique_domains])
            add_codelist('DOMAIN', domain_label, terms)
            print(f"  DOMAIN: {len(terms)} terms (merged from all datasets)")

        # 1b. TESTCD/TEST and PARMCD/PARM pairs - extract from XPT unique values
        for prefix, info in testcd_test_pairs.items():
            domain = info.get('domain', '')
            df = get_xpt(domain) if domain else None
            if df is None:
                continue

            testcd_var = info.get('testcd_var', '').upper()
            test_var = info.get('test_var', '').upper()
            testcd_subm = info.get('testcd_subm', '')
            test_subm = info.get('test_subm', '')
            testcd_vcd = f"{domain}.{testcd_var}" if testcd_var else ''
            test_vcd = f"{domain}.{test_var}" if test_var else ''

            if testcd_var in df.columns:
                if test_var and test_var in df.columns:
                    pairs = df[[testcd_var, test_var]].drop_duplicates()
                    pairs = pairs.dropna(subset=[testcd_var])
                    pairs = pairs[pairs[testcd_var].astype(str).str.strip() != '']
                    pairs = pairs.sort_values(testcd_var).reset_index(drop=True)

                    testcd_terms = []
                    test_terms = []
                    for _, r in pairs.iterrows():
                        cd_val = str(r[testcd_var]).strip()
                        t_val = str(r[test_var]).strip()
                        if cd_val:
                            testcd_terms.append({'code': cd_val, 'codeDes': t_val})
                            test_terms.append({'code': t_val, 'codeDes': ''})

                    add_codelist(testcd_vcd, info.get('testcd_label', ''), testcd_terms,
                                 subm_val=testcd_subm)
                    add_codelist(test_vcd, info.get('test_label', ''), test_terms,
                                 subm_val=test_subm)
                    print(f"  {testcd_vcd}: {len(testcd_terms)} terms from {domain}.{testcd_var}")
                else:
                    vals = df[testcd_var].dropna().astype(str).str.strip()
                    vals = vals[vals != ''].unique()
                    terms = [{'code': v, 'codeDes': ''} for v in sorted(vals)]
                    add_codelist(testcd_vcd, info.get('testcd_label', ''), terms,
                                 subm_val=testcd_subm)
                    print(f"  {testcd_vcd}: {len(terms)} terms from {domain}.{testcd_var}")

        # 1c. General variables - each domain.variable extracted independently
        for info in general_vars:
            domain = info['domain']
            variable = info['variable']
            subm_val = info['subm_val']
            label = info['label']
            vcd = f"{domain}.{variable}"

            df = get_xpt(domain)
            if df is None:
                continue

            col = variable.upper()
            if col not in df.columns:
                col = subm_val.upper()
            if col not in df.columns:
                continue

            vals = df[col].dropna().astype(str).str.strip()
            vals = vals[vals != ''].unique()
            vals = sorted(vals)

            if not vals:
                print(f"  {vcd}: skipped (no data in {domain}.{col})")
                continue

            paired_col = None
            if col.endswith('CD'):
                base = col[:-2]
                if base in df.columns:
                    paired_col = base

            terms = []
            if paired_col and paired_col in df.columns:
                pairs = df[[col, paired_col]].drop_duplicates()
                pairs = pairs.dropna(subset=[col])
                pairs = pairs[pairs[col].astype(str).str.strip() != '']
                pairs = pairs.sort_values(col).reset_index(drop=True)
                for _, r in pairs.iterrows():
                    code_val = str(r[col]).strip()
                    des_val = str(r[paired_col]).strip()
                    if code_val:
                        terms.append({'code': code_val, 'codeDes': des_val})
            else:
                terms = [{'code': v, 'codeDes': ''} for v in vals]

            data_type = 'Char'
            if _infer_data_type([t['code'] for t in terms]) != 'text':
                data_type = 'Num'

            terms = _sort_terms(terms)
            add_codelist(vcd, label, terms, data_type, subm_val=subm_val)
            print(f"  {vcd}: {len(terms)} terms from {domain}.{col}")

        # ── Part 2: VLM-level codelists ──────────────────────────────────
        print("\n=== Part 2: VLM-level codelists ===")
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            sql = ("SELECT id, dataset, variable, where_clause, label "
                   "FROM sas_vlm_data WHERE project_id = %s")
            params = [project_id]
            if username:
                sql += " AND username = %s"
                params.append(username)
            sql += " ORDER BY dataset, sort_order"
            cur.execute(sql, params)
            vlm_rows = cur.fetchall()

        vlm_codelist_ids = []

        for row in vlm_rows:
            ds = (row.get('dataset') or '').upper()
            variable = (row.get('variable') or '').upper()
            wc = row.get('where_clause') or ''
            vlm_label = row.get('label') or ''
            vlm_id = row['id']

            df = get_xpt(ds)
            if df is None:
                continue

            parsed = parse_where_clause(wc)
            if not parsed:
                continue

            filter_col, filter_val = parsed

            # Determine column to check: prefer --STRESC over --ORRES
            stresc_col = None
            if variable.endswith('ORRES'):
                stresc_col = variable[:-5] + 'STRESC'

            check_col = None
            if stresc_col and stresc_col in df.columns:
                check_col = stresc_col
            elif variable in df.columns:
                check_col = variable
            else:
                continue

            # Apply where clause filter
            if filter_col in df.columns:
                src = df[filter_col]
                if src.dtype == object:
                    mask = src.str.strip() == filter_val
                else:
                    try:
                        mask = src == float(filter_val)
                    except (ValueError, TypeError):
                        mask = src.astype(str).str.strip() == filter_val
                filtered = df.loc[mask, check_col].dropna()
            else:
                continue

            vals = filtered.astype(str).str.strip()
            vals = vals[vals != ''].unique()

            if len(vals) == 0:
                continue

            all_numeric = all(_is_numeric(v) for v in vals)
            if all_numeric:
                continue

            codelist_id = f"{ds}.{variable}.{filter_val}"
            terms = _sort_terms([{'code': v, 'codeDes': ''} for v in sorted(vals)])
            add_codelist(codelist_id, vlm_label, terms)
            vlm_codelist_ids.append((vlm_id, codelist_id))
            print(f"  {codelist_id}: {len(terms)} terms")

        # Update sas_vlm_data.codelist column
        if vlm_codelist_ids:
            with conn.cursor() as cur:
                update_sql = "UPDATE sas_vlm_data SET codelist=%s, updated_time=NOW() WHERE id=%s"
                for vlm_id, cl_id in vlm_codelist_ids:
                    cur.execute(update_sql, (cl_id, vlm_id))
                # Clear codelist for rows that have no codelist
                all_vlm_ids = set(r['id'] for r in vlm_rows)
                filled_ids = set(vid for vid, _ in vlm_codelist_ids)
                unfilled_ids = all_vlm_ids - filled_ids
                for uid in unfilled_ids:
                    cur.execute(update_sql, (None, uid))
            conn.commit()
            print(f"  Updated {len(vlm_codelist_ids)} VLM codelist references")

        # ── Part 3: Write to sas_codelist_data ───────────────────────────
        print("\n=== Writing codelist data to DB ===")
        with conn.cursor() as cur:
            if username:
                cur.execute("DELETE FROM sas_codelist_data WHERE project_id = %s AND username = %s "
                            "AND (created_by IS NULL OR created_by NOT IN ('extract_vlm_codelist'))",
                            (project_id, username))
            else:
                cur.execute("DELETE FROM sas_codelist_data WHERE project_id = %s "
                            "AND (created_by IS NULL OR created_by NOT IN ('extract_vlm_codelist'))",
                            (project_id,))
            deleted = cur.rowcount
            print(f"  Cleared {deleted} old codelist rows")

            insert_sql = """
            INSERT INTO sas_codelist_data
                (project_id, username, vcd, vlabel, type, cdnum, code, code_des, sort_order, created_by)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """

            total_inserted = 0
            global_order = 0
            for vcd in sorted(codelist_map.keys()):
                entry = codelist_map[vcd]
                terms = entry['terms']
                if not terms:
                    continue
                vlabel = entry['vlabel']
                dtype = entry['type']
                for idx, term in enumerate(terms, 1):
                    global_order += 1
                    cur.execute(insert_sql, (
                        project_id, username, vcd, vlabel, dtype,
                        idx, term['code'], term['codeDes'],
                        global_order, 'codelist_extractor'
                    ))
                    total_inserted += 1

        conn.commit()
        print(f"\n[DONE] Inserted {total_inserted} codelist rows for {len(codelist_map)} unique IDs.")

        # ── Part 3b: Update sas_project_spec.codelist to match vcd ───────
        print("\n=== Updating sas_project_spec.codelist references ===")
        spec_update_count = 0
        with conn.cursor() as cur:
            # Build domain.variable → vcd mapping from codelist_map
            dv_to_vcd: Dict[str, str] = {}
            for vcd, entry in codelist_map.items():
                if not entry['terms']:
                    continue
                dv_to_vcd[vcd.upper()] = vcd

            # Also map TESTCD/TEST pairs
            for prefix, info in testcd_test_pairs.items():
                domain = info.get('domain', '')
                for var_key in ('testcd_var', 'test_var'):
                    var = info.get(var_key, '').upper()
                    if var:
                        key = f"{domain}.{var}"
                        if key.upper() in dv_to_vcd:
                            dv_to_vcd[key.upper()] = dv_to_vcd[key.upper()]

            for row in spec_rows:
                subm_val = (row.get('cdisc_submission_value') or '').strip()
                if not subm_val:
                    continue
                domain = (row.get('domain') or '').strip().upper()
                variable = (row.get('variable') or '').strip().upper()
                dv_key = f"{domain}.{variable}".upper()

                if subm_val.upper() == 'DOMAIN':
                    new_cl = 'DOMAIN'
                elif subm_val.upper() in ('MEDDRA', 'WHODRUG'):
                    continue
                elif dv_key in dv_to_vcd:
                    new_cl = dv_to_vcd[dv_key]
                else:
                    new_cl = None

                old_cl = (row.get('codelist') or '').strip()
                if new_cl != old_cl:
                    if username:
                        cur.execute(
                            "UPDATE sas_project_spec SET codelist=%s, updated_time=NOW() "
                            "WHERE project_id=%s AND username=%s AND domain=%s AND variable=%s",
                            (new_cl, project_id, username, domain, variable))
                    else:
                        cur.execute(
                            "UPDATE sas_project_spec SET codelist=%s, updated_time=NOW() "
                            "WHERE project_id=%s AND domain=%s AND variable=%s",
                            (new_cl, project_id, domain, variable))
                    spec_update_count += 1
        conn.commit()
        print(f"  Updated {spec_update_count} spec codelist references")

        # ── Part 4: Write MEDDRA / WHODRUG to sas_dictionaries_data ──────
        if dict_ids_seen:
            print("\n=== Writing dictionaries data to DB ===")
            DICT_META = {
                'MEDDRA': {'name': 'Adverse Event Dictionary', 'dictionary': 'MEDDRA'},
                'WHODRUG': {'name': 'Drug Dictionary', 'dictionary': 'WHODRUG'},
            }
            with conn.cursor() as cur:
                cur.execute(
                    "DELETE FROM sas_dictionaries_data WHERE project_id = %s AND username = %s",
                    (project_id, username))
                d_order = 0
                for did in sorted(dict_ids_seen):
                    meta = DICT_META.get(did, {})
                    d_order += 1
                    cur.execute(
                        "INSERT INTO sas_dictionaries_data "
                        "(project_id, username, dictionary_id, name, data_type, dictionary, version, sort_order, created_by) "
                        "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)",
                        (project_id, username, did,
                         meta.get('name', ''), 'text',
                         meta.get('dictionary', did), '',
                         d_order, 'codelist_extractor'))
                    print(f"  Dictionary written: {did}")
            conn.commit()

        return True

    except Exception as e:
        conn.rollback()
        print(f"[ERROR] {e}")
        import traceback
        traceback.print_exc()
        return False
    finally:
        conn.close()


if __name__ == '__main__':
    project_id = os.environ.get('PROJECT_ID', 'P001')
    upload_base = os.environ.get('UPLOAD_BASE_PATH', 'C:/Project_Web/019_defineXML/uploads')
    data_path = os.environ.get('DATA_PATH', os.path.join(upload_base, project_id, 'xpt'))
    username = os.environ.get('USERNAME_CONTEXT', '')

    print("=== Codelist Extractor ===")
    print(f"  Project: {project_id}")
    print(f"  Data: {data_path}")
    print(f"  User: {username or '(unspecified)'}")

    success = run(project_id, data_path, username)
    sys.exit(0 if success else 1)
