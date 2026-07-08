#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
VLM Codelist Filler
Reads XPT --STRESC column for each VLM row, extracts unique values as a codelist,
then fills the codelist column with the identifier format: code_domain.Variable
If --STRESC has distinct non-numeric values, the variable has a codelist.
"""

import os
import sys
import re
import pymysql
import pyreadstat
import pandas as pd
from pathlib import Path
from typing import Dict, Optional, Tuple

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
    matches = list(data_path.glob(f"*_{dataset}.xpt"))
    if matches:
        return matches[0]
    for p in data_path.glob("*.xpt"):
        stem = p.stem.lower()
        if stem == dataset.lower() or stem.endswith(f"_{dataset.lower()}"):
            return p
    return None


def read_xpt_with_meta(xpt_path: Path) -> Tuple[pd.DataFrame, object]:
    df, meta = pyreadstat.read_xport(str(xpt_path))
    df.columns = df.columns.str.upper()
    return df, meta


def parse_where_clause(wc: str) -> Optional[Tuple[str, str]]:
    if not wc or not wc.strip():
        return None
    m = re.match(r'(\w+)\s+EQ\s+"([^"]*)"', wc.strip())
    if m:
        return m.group(1).upper(), m.group(2)
    return None


def _derive_stresc_col(dataset: str, variable: str) -> Optional[str]:
    var = variable.upper()
    if var.endswith('ORRES'):
        return var[:-5] + 'STRESC'
    return None


def _has_codelist(df: pd.DataFrame, col: str, filter_col: str, filter_val: str) -> bool:
    """
    Determine if a column has a codelist by checking if STRESC values
    contain non-numeric coded values (text codes, not pure numbers).
    """
    if col not in df.columns:
        return False

    if filter_col and filter_val and filter_col in df.columns:
        src = df[filter_col]
        if src.dtype == object:
            mask = src.str.strip() == filter_val
        else:
            try:
                mask = src == float(filter_val)
            except (ValueError, TypeError):
                mask = src.astype(str).str.strip() == filter_val
        vals = df.loc[mask, col].dropna()
    else:
        vals = df[col].dropna()

    if vals.empty:
        return False

    if vals.dtype != object:
        return False

    unique_vals = vals.astype(str).str.strip().unique()
    unique_vals = [v for v in unique_vals if v]

    if len(unique_vals) == 0:
        return False

    all_numeric = all(_is_numeric(v) for v in unique_vals)
    if all_numeric:
        return False

    return True


def _is_numeric(s: str) -> bool:
    try:
        float(s)
        return True
    except (ValueError, TypeError):
        return False


def run(project_id: str, data_path: str, username: str = ''):
    db_config = _get_db_config()
    data_dir = Path(data_path)
    if not data_dir.exists():
        print(f"[ERROR] XPT path not found: {data_dir}")
        return False

    conn = pymysql.connect(
        host=db_config['host'], user=db_config['user'],
        password=db_config['password'], database=db_config['database'],
        charset='utf8mb4', autocommit=True,
    )

    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            sql = "SELECT id, dataset, variable, where_clause FROM sas_vlm_data WHERE project_id = %s"
            params = [project_id]
            if username:
                sql += " AND username = %s"
                params.append(username)
            sql += " ORDER BY sort_order"
            cur.execute(sql, params)
            vlm_rows = cur.fetchall()

        if not vlm_rows:
            print("[WARN] No VLM rows found.")
            return True

        print(f"[INFO] {len(vlm_rows)} VLM rows to check for codelists.")

        xpt_cache: Dict[str, Tuple[pd.DataFrame, object]] = {}
        datasets = set(r['dataset'].upper() for r in vlm_rows if r.get('dataset'))
        for ds in datasets:
            xpt_file = find_xpt_file(data_dir, ds.lower()) or find_xpt_file(data_dir, ds.upper())
            if xpt_file:
                try:
                    df, meta = read_xpt_with_meta(xpt_file)
                    xpt_cache[ds] = (df, meta)
                    print(f"  [OK] {ds}.xpt loaded")
                except Exception as e:
                    print(f"  [ERR] {ds}.xpt: {e}")

        update_sql = "UPDATE sas_vlm_data SET codelist=%s, updated_time=NOW() WHERE id=%s"
        filled = 0

        with conn.cursor() as cur:
            for row in vlm_rows:
                ds = (row['dataset'] or '').upper()
                variable = (row['variable'] or '').upper()
                wc = row.get('where_clause') or ''
                row_id = row['id']

                # TS domain does not need codelist
                if ds == 'TS':
                    cur.execute(update_sql, (None, row_id))
                    continue

                if ds not in xpt_cache:
                    cur.execute(update_sql, (None, row_id))
                    continue

                df, _ = xpt_cache[ds]
                parsed = parse_where_clause(wc)
                fc = parsed[0] if parsed else None
                fv = parsed[1] if parsed else None

                stresc_col = _derive_stresc_col(ds, variable)
                check_col = stresc_col if (stresc_col and stresc_col in df.columns) else variable

                if _has_codelist(df, check_col, fc, fv):
                    # Double-check: ensure there is at least one non-numeric distinct value
                    if fc and fv and fc in df.columns:
                        src = df[fc]
                        if src.dtype == object:
                            mask = src.str.strip() == fv
                        else:
                            try:
                                mask = src == float(fv)
                            except (ValueError, TypeError):
                                mask = src.astype(str).str.strip() == fv
                        raw_vals = df.loc[mask, check_col].dropna().astype(str).str.strip()
                    else:
                        raw_vals = df[check_col].dropna().astype(str).str.strip()
                    non_numeric_vals = [v for v in raw_vals if v and not _is_numeric(v)]
                    if not non_numeric_vals:
                        cur.execute(update_sql, (None, row_id))
                        continue
                    filter_val = fv if fv else ''
                    codelist_id = f"{ds}.{variable}.{filter_val}" if filter_val else f"{ds}.{variable}"
                    cur.execute(update_sql, (codelist_id, row_id))
                    filled += 1
                else:
                    cur.execute(update_sql, (None, row_id))

        print(f"[DONE] Codelist filled for {filled} rows.")
        return True

    except Exception as e:
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

    print(f"VLM Codelist Filler")
    print(f"  Project: {project_id}, Data: {data_path}, User: {username}")

    success = run(project_id, data_path, username)
    sys.exit(0 if success else 1)
