#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
VLM XPT Metadata Extractor
Reads XPT files with pyreadstat to extract Data Type, Length,
Significant Digits, and Format for each VLM row.

Priority: use --STRESC column first, fallback to --ORRES.
E.g. for dataset LB, variable LBORRES → check LBSTRESC first, then LBORRES.
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


def parse_where_clause(where_clause: str) -> Optional[Tuple[str, str]]:
    if not where_clause or not where_clause.strip():
        return None
    m = re.match(r'(\w+)\s+EQ\s+"([^"]*)"', where_clause.strip())
    if m:
        return m.group(1).upper(), m.group(2)
    return None


def _derive_stresc_col(dataset: str, variable: str) -> Optional[str]:
    """
    Derive the --STRESC column name from the variable.
    E.g. LBORRES → LBSTRESC, VSORRES → VSSTRESC, TSVAL → TSVAL (no STRESC).
    Only applies when variable ends with ORRES.
    """
    ds = dataset.upper()
    var = variable.upper()
    if var.endswith('ORRES'):
        prefix = var[:-5]  # LBORRES → LB
        return prefix + 'STRESC'
    if var.endswith('VAL'):
        prefix = var[:-3]
        return prefix + 'STRESN'
    return None


def _filter_series(df: pd.DataFrame, col: str, filter_col: str, filter_val: str) -> pd.Series:
    """Filter df by Where Clause and return the target column values."""
    if filter_col and filter_val and filter_col in df.columns:
        src = df[filter_col]
        if src.dtype == object:
            mask = src.str.strip() == filter_val
        else:
            try:
                mask = src == float(filter_val)
            except (ValueError, TypeError):
                mask = src.astype(str).str.strip() == filter_val
        return df.loc[mask, col].dropna()
    return df[col].dropna()


def _get_col_meta(meta, col_name: str) -> Dict[str, str]:
    """Extract length/format from pyreadstat metadata for a column."""
    result = {}
    col_upper = col_name.upper()
    names_upper = [c.upper() for c in meta.column_names]
    if col_upper not in names_upper:
        return result
    idx = names_upper.index(col_upper)

    if hasattr(meta, 'variable_display_width') and meta.variable_display_width:
        if isinstance(meta.variable_display_width, dict):
            w = meta.variable_display_width.get(meta.column_names[idx])
            if w:
                result['length'] = str(int(w))
        elif isinstance(meta.variable_display_width, (list, tuple)) and idx < len(meta.variable_display_width):
            w = meta.variable_display_width[idx]
            if w:
                result['length'] = str(int(w))

    if hasattr(meta, 'variable_format') and meta.variable_format:
        if isinstance(meta.variable_format, dict):
            f = meta.variable_format.get(meta.column_names[idx], '')
        elif isinstance(meta.variable_format, (list, tuple)) and idx < len(meta.variable_format):
            f = meta.variable_format[idx] or ''
        else:
            f = ''
        if f:
            result['format'] = f
    return result


def infer_metadata(df: pd.DataFrame, meta, target_col: str,
                   filter_col: Optional[str], filter_val: Optional[str]) -> Dict[str, str]:
    """
    Infer Data Type, Length, Significant Digits, Format from filtered data.
    target_col is already resolved (STRESC or ORRES).
    """
    result = {'data_type': '', 'length': '', 'significant_digits': '', 'format': ''}

    if target_col not in df.columns:
        return result

    filtered = _filter_series(df, target_col, filter_col, filter_val)
    col_meta = _get_col_meta(meta, target_col)

    if filtered.empty:
        if col_meta:
            result.update(col_meta)
        return result

    if df[target_col].dtype == object:
        str_vals = filtered.astype(str)

        all_numeric = True
        max_decimals = 0
        has_decimal = False
        for v in str_vals:
            v_stripped = v.strip()
            if not v_stripped:
                continue
            try:
                fv = float(v_stripped)
                if '.' in v_stripped:
                    has_decimal = True
                    dec_part = v_stripped.rstrip('0').split('.')[-1]
                    if dec_part:
                        max_decimals = max(max_decimals, len(dec_part))
            except (ValueError, TypeError):
                all_numeric = False

        if all_numeric and len(str_vals) > 0:
            if has_decimal:
                result['data_type'] = 'float'
                result['significant_digits'] = str(max_decimals) if max_decimals > 0 else ''
            else:
                result['data_type'] = 'integer'
                result['significant_digits'] = ''
        else:
            result['data_type'] = 'text'
            result['significant_digits'] = ''

        max_len = int(str_vals.str.len().max()) if len(str_vals) > 0 else 0
        result['length'] = str(max(max_len, 1))

        if result['data_type'] == 'float' and max_decimals > 0:
            result['format'] = f"{result['length']}.{max_decimals}"
        else:
            result['format'] = ''
    else:
        vals = filtered.dropna()
        if len(vals) == 0:
            result['data_type'] = 'float'
            result['length'] = col_meta.get('length', '8')
            return result

        has_decimal = False
        max_decimals = 0
        for v in vals:
            if pd.isna(v):
                continue
            fv = float(v)
            if fv != int(fv):
                has_decimal = True
            s = str(v)
            if '.' in s:
                dec_part = s.rstrip('0').split('.')[-1]
                if dec_part:
                    max_decimals = max(max_decimals, len(dec_part))

        if has_decimal:
            result['data_type'] = 'float'
            result['significant_digits'] = str(max_decimals) if max_decimals > 0 else ''
        else:
            result['data_type'] = 'integer'
            result['significant_digits'] = ''

        result['length'] = col_meta.get('length', '8')

        if has_decimal and max_decimals > 0:
            result['format'] = f"8.{max_decimals}"
        else:
            result['format'] = ''

    return result


def run(project_id: str, data_path: str, username: str = ''):
    db_config = _get_db_config()
    data_dir = Path(data_path)
    if not data_dir.exists():
        print(f"[ERROR] XPT data path does not exist: {data_dir}")
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

        print(f"[INFO] {len(vlm_rows)} VLM rows to process.")

        xpt_cache: Dict[str, Tuple[pd.DataFrame, object]] = {}
        datasets = set(r['dataset'].upper() for r in vlm_rows if r.get('dataset'))
        for ds in datasets:
            xpt_file = find_xpt_file(data_dir, ds.lower()) or find_xpt_file(data_dir, ds.upper())
            if xpt_file:
                try:
                    df, meta = read_xpt_with_meta(xpt_file)
                    xpt_cache[ds] = (df, meta)
                    print(f"  [OK] {ds}.xpt: {len(df)} rows, {len(df.columns)} cols")
                except Exception as e:
                    print(f"  [ERR] {xpt_file}: {e}")
            else:
                print(f"  [SKIP] {ds}.xpt not found")

        update_sql = (
            "UPDATE sas_vlm_data SET data_type=%s, length=%s, "
            "significant_digits=%s, format=%s, updated_time=NOW() WHERE id=%s"
        )
        updated = 0
        skipped = 0

        with conn.cursor() as cur:
            for row in vlm_rows:
                ds = (row['dataset'] or '').upper()
                variable = (row['variable'] or '').upper()
                wc = row.get('where_clause') or ''
                row_id = row['id']

                if ds not in xpt_cache:
                    skipped += 1
                    continue

                df, meta = xpt_cache[ds]
                parsed = parse_where_clause(wc)
                fc = parsed[0] if parsed else None
                fv = parsed[1] if parsed else None

                stresc_col = _derive_stresc_col(ds, variable)
                if stresc_col and stresc_col in df.columns:
                    target = stresc_col
                else:
                    target = variable

                info = infer_metadata(df, meta, target, fc, fv)

                cur.execute(update_sql, (
                    info['data_type'] or None,
                    info['length'] or None,
                    info['significant_digits'] or None,
                    info['format'] or None,
                    row_id,
                ))
                updated += 1

        print(f"[DONE] Updated {updated}, skipped {skipped}.")
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

    print(f"VLM XPT Metadata Extractor")
    print(f"  Project: {project_id}, Data: {data_path}, User: {username}")

    success = run(project_id, data_path, username)
    sys.exit(0 if success else 1)
