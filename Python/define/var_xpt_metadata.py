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
import json
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


def read_xpt_with_meta(xpt_path: Path) -> Tuple[pd.DataFrame, object, str]:
    """Read XPT using deterministic encoding fallbacks for legacy SAS labels."""
    errors = []
    for encoding in (None, 'latin1', 'cp1252', 'gb18030'):
        try:
            kwargs = {'encoding': encoding} if encoding else {}
            df, meta = pyreadstat.read_xport(str(xpt_path), **kwargs)
            df.columns = df.columns.str.upper()
            return df, meta, encoding or 'auto'
        except UnicodeDecodeError as exc:
            errors.append(f"{encoding or 'auto'}: {exc}")
    raise RuntimeError('all supported encodings failed: ' + ' | '.join(errors))


def _get_col_meta(meta, col_name: str) -> Dict[str, str]:
    """Extract declared type, storage length and format from XPT metadata."""
    result = {}
    col_upper = col_name.upper()
    names_upper = [c.upper() for c in meta.column_names]
    if col_upper not in names_upper:
        return result
    idx = names_upper.index(col_upper)

    original_name = meta.column_names[idx]
    storage_width = getattr(meta, 'variable_storage_width', None)
    if isinstance(storage_width, dict):
        width = storage_width.get(original_name)
        if width:
            result['length'] = str(int(width))
    elif isinstance(storage_width, (list, tuple)) and idx < len(storage_width):
        width = storage_width[idx]
        if width:
            result['length'] = str(int(width))

    readstat_types = getattr(meta, 'readstat_variable_types', None)
    if isinstance(readstat_types, dict):
        declared_type = (readstat_types.get(original_name) or '').lower()
        if declared_type == 'string':
            result['data_type'] = 'text'
        elif declared_type in ('double', 'float', 'int8', 'int16', 'int32'):
            result['data_type'] = 'float'

    if hasattr(meta, 'variable_format') and meta.variable_format:
        if isinstance(meta.variable_format, dict):
            f = meta.variable_format.get(original_name, '')
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
    result.update(col_meta)
    series = df[col].dropna()

    if series.empty:
        return result

    if col_meta.get('data_type') == 'text':
        str_vals = series.astype(str).str.strip()
        str_vals = str_vals[str_vals != '']
        if not result['length']:
            max_len = int(str_vals.str.len().max()) if len(str_vals) > 0 else 1
            result['length'] = str(max(max_len, 1))
    else:
        vals = series.dropna()
        if len(vals) == 0:
            result['data_type'] = col_meta.get('data_type', 'float')
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
        charset='utf8mb4', autocommit=False,
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
            print('[RESULT] ' + json.dumps({
                'success': False, 'updated': 0, 'skipped': 0,
                'datasets': [], 'errors': ['No spec rows found']
            }, ensure_ascii=False))
            return False

        print(f"[INFO] {len(spec_rows)} spec rows to process.")

        xpt_cache: Dict[str, Tuple[pd.DataFrame, object, str]] = {}
        dataset_results = []
        errors = []
        datasets = set(r['domain'].upper() for r in spec_rows if r.get('domain'))
        for ds in datasets:
            xpt_file = find_xpt_file(data_dir, ds.lower()) or find_xpt_file(data_dir, ds.upper())
            if xpt_file:
                try:
                    df, meta, encoding = read_xpt_with_meta(xpt_file)
                    xpt_cache[ds] = (df, meta, encoding)
                    dataset_results.append({
                        'dataset': ds, 'status': 'ok', 'encoding': encoding,
                        'rows': len(df), 'columns': len(df.columns)
                    })
                    print(f"  [OK] {ds}.xpt: {len(df)} rows, {len(df.columns)} cols, encoding={encoding}")
                except Exception as e:
                    errors.append(f"{ds}: {e}")
                    dataset_results.append({'dataset': ds, 'status': 'error', 'error': str(e)})
                    print(f"  [ERR] {ds}.xpt: {e}")
            else:
                errors.append(f"{ds}: XPT file not found")
                dataset_results.append({'dataset': ds, 'status': 'missing'})
                print(f"  [SKIP] {ds}.xpt not found")

        if not xpt_cache:
            print("[ERROR] No readable XPT files matched the project datasets.")
            print('[RESULT] ' + json.dumps({
                'success': False, 'updated': 0, 'skipped': len(spec_rows),
                'datasets': dataset_results, 'errors': errors
            }, ensure_ascii=False))
            return False

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

                df, meta, _encoding = xpt_cache[ds]
                if variable not in df.columns:
                    skipped += 1
                    continue

                info = infer_var_metadata(df, meta, variable)
                if not info['data_type'] and not info['length']:
                    skipped += 1
                    continue

                cur.execute(update_sql, (
                    info['data_type'] or None,
                    info['length'] or None,
                    info['significant_digits'] or None,
                    info['format'] or None,
                    row_id,
                ))
                updated += 1

        conn.commit()
        print(f"[DONE] Updated {updated}, skipped {skipped}.")
        print('[RESULT] ' + json.dumps({
            'success': updated > 0,
            'partial': bool(errors) or skipped > 0,
            'updated': updated,
            'skipped': skipped,
            'datasets': dataset_results,
            'errors': errors
        }, ensure_ascii=False))
        if updated == 0:
            print("[ERROR] XPT files were readable, but no Spec variables matched.")
            return False
        return True

    except Exception as e:
        conn.rollback()
        print(f"[ERROR] {e}")
        print('[RESULT] ' + json.dumps({
            'success': False, 'updated': 0, 'skipped': 0,
            'datasets': [], 'errors': [str(e)]
        }, ensure_ascii=False))
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
