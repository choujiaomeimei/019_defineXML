#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Variables XPT Metadata Extractor
Reads XPT files with pyreadstat to extract Data Type, Length,
Significant Digits, and Format for each variable in sas_project_spec.

Borrows the inference logic from vlm_xpt_metadata.py but operates
at the Variables level (no Where Clause filtering).
"""

import os
import sys
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


def infer_var_metadata(df: pd.DataFrame, meta, variable: str) -> Dict[str, str]:
    """
    Infer Data Type, Length, Significant Digits, Format for a variable
    from the full (unfiltered) column data.
    """
    result = {'data_type': '', 'length': '', 'significant_digits': '', 'format': ''}

    col = variable.upper()
    if col not in df.columns:
        return result

    col_meta = _get_col_meta(meta, col)
    series = df[col].dropna()

    if series.empty:
        if col_meta:
            result.update(col_meta)
        return result

    if df[col].dtype == object:
        str_vals = series.astype(str).str.strip()
        str_vals = str_vals[str_vals != '']

        if str_vals.empty:
            result['data_type'] = 'text'
            result['length'] = '1'
            return result

        all_numeric = True
        max_decimals = 0
        has_decimal = False
        for v in str_vals:
            try:
                float(v)
                if '.' in v:
                    has_decimal = True
                    dec_part = v.rstrip('0').split('.')[-1]
                    if dec_part:
                        max_decimals = max(max_decimals, len(dec_part))
            except (ValueError, TypeError):
                all_numeric = False

        if all_numeric:
            if has_decimal:
                result['data_type'] = 'float'
                result['significant_digits'] = str(max_decimals) if max_decimals > 0 else ''
            else:
                result['data_type'] = 'integer'
        else:
            result['data_type'] = 'text'

        max_len = int(str_vals.str.len().max()) if len(str_vals) > 0 else 0
        result['length'] = str(max(max_len, 1))

        if result['data_type'] == 'float' and max_decimals > 0:
            result['format'] = f"{result['length']}.{max_decimals}"
    else:
        vals = series.dropna()
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

        result['length'] = col_meta.get('length', '8')

        if has_decimal and max_decimals > 0:
            result['format'] = f"8.{max_decimals}"

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
            sql = "SELECT id, domain, variable FROM sas_project_spec WHERE project_id = %s"
            params = [project_id]
            if username:
                sql += " AND username = %s"
                params.append(username)
            sql += " ORDER BY domain, sort_order"
            cur.execute(sql, params)
            spec_rows = cur.fetchall()

        if not spec_rows:
            print("[WARN] No spec rows found.")
            return True

        print(f"[INFO] {len(spec_rows)} spec rows to process.")

        xpt_cache: Dict[str, Tuple[pd.DataFrame, object]] = {}
        datasets = set(r['domain'].upper() for r in spec_rows if r.get('domain'))
        for ds in datasets:
            xpt_file = find_xpt_file(data_dir, ds.lower()) or find_xpt_file(data_dir, ds.upper())
            if xpt_file:
                try:
                    df, meta = read_xpt_with_meta(xpt_file)
                    xpt_cache[ds] = (df, meta)
                    print(f"  [OK] {ds}.xpt: {len(df)} rows, {len(df.columns)} cols")
                except Exception as e:
                    print(f"  [ERR] {ds}.xpt: {e}")
            else:
                print(f"  [SKIP] {ds}.xpt not found")

        update_sql = (
            "UPDATE sas_project_spec SET type=%s, length=%s, "
            "significant_digits=%s, format=%s, updated_time=NOW(), "
            "updated_by='var_xpt_extract' WHERE id=%s"
        )
        updated = 0
        skipped = 0

        with conn.cursor() as cur:
            for row in spec_rows:
                ds = (row['domain'] or '').upper()
                variable = (row['variable'] or '').upper()
                row_id = row['id']

                if ds not in xpt_cache:
                    skipped += 1
                    continue

                df, meta = xpt_cache[ds]
                info = infer_var_metadata(df, meta, variable)

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

    print("Variables XPT Metadata Extractor")
    print(f"  Project: {project_id}, Data: {data_path}, User: {username}")

    success = run(project_id, data_path, username)
    sys.exit(0 if success else 1)
