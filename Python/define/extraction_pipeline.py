#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Unified Extraction Pipeline
Runs Pages + VLM + CodeList extraction in sequence for a given project.
Stores results in the database (sas_vlm_data, sas_codelist_data, sas_pages_data).
"""

import os
import sys
import json
from pathlib import Path
from typing import Dict, Optional

sys.path.append(str(Path(__file__).parent))
sys.path.append(str(Path(__file__).parent.parent / 'pgm'))


def _get_db_config() -> Dict[str, str]:
    return {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'user': os.environ.get('DB_USER', 'root'),
        'password': os.environ.get('DB_PASSWORD', '123123'),
        'database': os.environ.get('DB_NAME', 'define_db'),
    }


def _find_spec_path(upload_base: str, project_id: str, python_base: str) -> str:
    synced = os.path.join(upload_base, project_id, 'output', f'spec_synced_{project_id}.xlsx')
    if os.path.isfile(synced):
        print(f"  [spec] 使用数据库同步文件: {synced}")
        return synced

    spec_dir = os.path.join(upload_base, project_id, 'project-spec')
    if os.path.isdir(spec_dir):
        for f in os.listdir(spec_dir):
            if f.lower().endswith(('.xlsx', '.xls')):
                path = os.path.join(spec_dir, f)
                print(f"  [spec] 回退到原始上传文件: {path}")
                return path
    fallback = os.path.join(python_base, 'define', '项目Spec', 'spec.xlsx')
    print(f"  [spec] 回退到默认Spec路径: {fallback}")
    return fallback


def _save_pages_to_db(pages_df, project_id: str, db_config: Dict[str, str], username: str = None):
    """Insert pages summary data into sas_pages_data table."""
    import pymysql
    if pages_df is None or pages_df.empty:
        print("  No pages data to save.")
        return

    username = username or os.environ.get('USERNAME_CONTEXT', '')

    conn = None
    try:
        conn = pymysql.connect(
            host=db_config['host'], user=db_config['user'],
            password=db_config['password'], database=db_config['database'],
            charset='utf8mb4', autocommit=True
        )
        with conn.cursor() as cur:
            if username:
                cur.execute("DELETE FROM sas_pages_data WHERE project_id = %s AND username = %s", (project_id, username))
            else:
                cur.execute("DELETE FROM sas_pages_data WHERE project_id = %s", (project_id,))
            sql = ("INSERT INTO sas_pages_data "
                   "(project_id, username, dataset, variable, where_clause, pages, origin, sort_order, created_by) "
                   "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)")
            for idx, row in pages_df.iterrows():
                wc = str(row.get('where_clause', '')) if row.get('where_clause') else ''
                cur.execute(sql, (
                    project_id,
                    username,
                    str(row.get('dataset', '')),
                    str(row.get('variable', '')),
                    wc if wc and wc != 'nan' else None,
                    str(row.get('pages', '')),
                    str(row.get('origin', '')),
                    idx,
                    'extraction_pipeline',
                ))
            print(f"  Saved {len(pages_df)} pages records to DB (username: {username or 'N/A'}).")
    except Exception as e:
        print(f"  Failed to save pages to DB: {e}")
    finally:
        if conn:
            conn.close()


def run_pipeline(project_id: str = None,
                 upload_base: str = None,
                 python_base: str = None,
                 steps: str = 'all',
                 username: str = None):
    """
    Run the extraction pipeline.

    Args:
        project_id:  Project identifier.
        upload_base: Root upload directory.
        python_base: Root Python directory.
        steps:       Comma-separated list of steps to run: vlm, codelist, pages, or 'all'.
        username:    Username for data isolation.
    """
    project_id = project_id or os.environ.get('PROJECT_ID', 'default')
    upload_base = upload_base or os.environ.get('UPLOAD_BASE_PATH', r'C:\Project_Web\019_defineXML\uploads')
    python_base = python_base or os.environ.get('PYTHON_BASE_PATH', r'C:\Project_Web\019_defineXML\Python')
    username = username or os.environ.get('USERNAME_CONTEXT', '')

    data_path = os.environ.get('DATA_PATH', os.path.join(upload_base, project_id, 'xpt'))
    spec_path = os.environ.get('SPEC_PATH', '') or _find_spec_path(upload_base, project_id, python_base)
    output_path = os.environ.get('OUTPUT_PATH', os.path.join(upload_base, project_id, 'output'))
    annots_path = os.environ.get('ANNOTS_PATH', os.path.join(output_path, 'Annots2.xlsx'))
    vlm_output = os.path.join(output_path, 'vlm_codelists.xlsx')

    db_config = _get_db_config()

    Path(output_path).mkdir(parents=True, exist_ok=True)

    run_steps = set(s.strip().lower() for s in steps.split(','))
    run_all = 'all' in run_steps

    print("=" * 60)
    print(f"  Extraction Pipeline  –  project: {project_id}")
    print("=" * 60)
    print(f"  XPT data:   {data_path}")
    print(f"  Spec:       {spec_path}")
    print(f"  Output:     {output_path}")
    print(f"  Annots:     {annots_path}")
    print(f"  Steps:      {steps}")
    print()

    # --- Step 1: VLM + CodeList ---
    if run_all or 'vlm' in run_steps or 'codelist' in run_steps:
        print("[Step 1] Extracting VLM + CodeList ...")
        try:
            from vlm_codelist_extractor import VLMCodeListExtractor
            extractor = VLMCodeListExtractor(data_path, spec_path, output_path, db_config, project_id, username=username)
            vlm_data, codelist_data = extractor.run()
            print(f"  VLM: {len(vlm_data)} rows,  CodeList: {len(codelist_data)} rows")
        except Exception as e:
            print(f"  VLM/CodeList extraction failed: {e}")
            import traceback; traceback.print_exc()

    # --- Step 2: Pages ---
    if run_all or 'pages' in run_steps:
        print("\n[Step 2] Extracting Pages ...")
        try:
            from find_variable_pages import extract_pages, build_pages_summary
            pages_output_file = os.path.join(output_path, 'variable_page_mapping.xlsx')
            results_df = extract_pages(
                annots_path=annots_path,
                spec_path=spec_path,
                vlm_path=vlm_output if os.path.exists(vlm_output) else None,
                output_path=pages_output_file,
            )
            summary_df = build_pages_summary(results_df)
            _save_pages_to_db(summary_df, project_id, db_config, username=username)
        except Exception as e:
            print(f"  Pages extraction failed: {e}")
            import traceback; traceback.print_exc()

    print("\n" + "=" * 60)
    print("  Pipeline complete.")
    print("=" * 60)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description='Extraction Pipeline')
    parser.add_argument('--project-id', type=str, default=None)
    parser.add_argument('--upload-base', type=str, default=None)
    parser.add_argument('--python-base', type=str, default=None)
    parser.add_argument('--steps', type=str, default='all',
                        help='Comma-separated: vlm,codelist,pages or all')
    parser.add_argument('--username', type=str, default=None,
                        help='Username for data isolation')
    args = parser.parse_args()

    run_pipeline(
        project_id=args.project_id,
        upload_base=args.upload_base,
        python_base=args.python_base,
        steps=args.steps,
        username=args.username,
    )
