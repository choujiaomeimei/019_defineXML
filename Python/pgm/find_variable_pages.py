#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Variable-to-Page mapping extractor.
Reads aCRF annotations (Annots2.xlsx), project spec, and VLM data to produce
a mapping of variables/VLM entries to their CRF page numbers.

Can be used as a module (call extract_pages()) or as a standalone script via
environment variables.
"""

import pandas as pd
import re
import os
from pathlib import Path
from typing import List, Dict, Optional


def parse_where_clause(where_clause):
    if pd.isna(where_clause) or not where_clause:
        return None, None
    match = re.search(r'(\w+)\s*(?:EQ|=)\s*["\']?([^"\']+)["\']?', str(where_clause), re.IGNORECASE)
    if match:
        return match.group(1).strip(), match.group(2).strip()
    return None, None


def find_variable_in_annotations(variable, value, annots_df):
    if annots_df is None or annots_df.empty:
        return []

    matches = []
    for _, row in annots_df.iterrows():
        content = str(row.get('Contents', '')).strip()
        if not content:
            continue

        patterns = [
            f"{variable}\\s*=\\s*{re.escape(value)}(?=\\s|$)",
            f"{variable}\\s*EQ\\s*{re.escape(value)}(?=\\s|$)",
            f"{variable}\\s*=\\s*[\"']{re.escape(value)}[\"'](?=\\s|$)",
            f"{variable}\\s*EQ\\s*[\"']{re.escape(value)}[\"'](?=\\s|$)",
            f"(?<![A-Za-z0-9]){re.escape(value)}(?![A-Za-z0-9]).*{variable}",
            f"{variable}.*(?<![A-Za-z0-9]){re.escape(value)}(?![A-Za-z0-9])",
        ]

        for pattern in patterns:
            if re.search(pattern, content, re.IGNORECASE):
                matches.append({
                    'page': row.get('page'),
                    'content': content,
                    'pattern_matched': pattern,
                    'variable': variable,
                    'value': value
                })
                break

    return matches


def find_domain_variable_in_annotations(domain, variable_name, supp_flag, annots_df):
    if annots_df is None or annots_df.empty:
        return []

    matches = []
    for _, row in annots_df.iterrows():
        content = str(row.get('Contents', '')).strip()
        if not content:
            continue

        if supp_flag == 'Y':
            patterns = [
                f"QNAM\\s*=\\s*{re.escape(variable_name)}(?=\\s|$)",
                f"QNAM\\s*=\\s*[\"']{re.escape(variable_name)}[\"'](?=\\s|$)",
                f"(?<![A-Za-z0-9]){re.escape(variable_name)}(?![A-Za-z0-9]).*QNAM",
                f"QNAM.*(?<![A-Za-z0-9]){re.escape(variable_name)}(?![A-Za-z0-9])",
            ]
        else:
            patterns = [
                f"(?<![A-Za-z0-9]){re.escape(variable_name)}(?![A-Za-z0-9])",
                f"{re.escape(variable_name)}\\s*=",
                f"when\\s+{re.escape(variable_name)}\\s*=",
            ]

        for pattern in patterns:
            if re.search(pattern, content, re.IGNORECASE):
                matches.append({
                    'page': row.get('page'),
                    'content': content,
                    'pattern_matched': pattern,
                    'domain': domain,
                    'variable': variable_name,
                    'supp': supp_flag
                })
                break

    return matches


def _find_col(df: pd.DataFrame, aliases: list) -> Optional[str]:
    """Find the first column matching any alias (case-insensitive)."""
    norm = {c.strip().lower().replace(' ', '_').replace('-', '_'): c for c in df.columns}
    for a in aliases:
        key = a.strip().lower().replace(' ', '_').replace('-', '_')
        if key in norm:
            return norm[key]
    return None


def extract_pages(annots_path: str, spec_path: str, vlm_path: str = None,
                  output_path: str = None) -> pd.DataFrame:
    """
    Extract variable-to-page mappings.

    Args:
        annots_path: Path to Annots2.xlsx (aCRF annotations output)
        spec_path:   Path to the project spec Excel file
        vlm_path:    Path to vlm_codelists.xlsx (optional – also reads VLM sheet)
        output_path: Where to write variable_page_mapping.xlsx. If None, no file is written.

    Returns:
        DataFrame with columns: Type, Domain/Dataset, Variable, SUPP,
        Where_Clause, Parsed_Variable, Parsed_Value, Found_Page, Found_Content
    """
    annots_df = None
    domains_df = None
    vlm_df = None

    # --- Read annotations ---
    try:
        annots_df = pd.read_excel(annots_path)
        print(f"Annots2.xlsx loaded: {len(annots_df)} rows")
    except Exception as e:
        print(f"Failed to read Annots2: {e}")

    # --- Read spec (all domain sheets) ---
    try:
        skip_sheets = {'toc', 'codelists', 'vlm', 'supp', 'annots', 'dm_bk'}
        excel_file = pd.ExcelFile(spec_path)
        parts = []
        for sheet_name in excel_file.sheet_names:
            if sheet_name.lower() in skip_sheets:
                continue
            if sheet_name.upper().startswith('SUPP'):
                continue
            df = excel_file.parse(sheet_name)
            col_var = _find_col(df, ['Variable Name', 'Variable_Name', 'variable', 'Variable'])
            if col_var is None:
                continue
            col_supp = _find_col(df, ['SUPP', 'supp'])
            part = pd.DataFrame({
                '_domain': sheet_name,
                '_var_name': df[col_var],
                '_supp': df[col_supp].fillna('N') if col_supp else 'N',
            })
            parts.append(part)
        if parts:
            domains_df = pd.concat(parts, ignore_index=True)
    except Exception as e:
        print(f"Failed to read spec: {e}")

    # --- Read VLM ---
    if vlm_path and Path(vlm_path).exists():
        try:
            vlm_xf = pd.ExcelFile(vlm_path)
            if 'VLM' in vlm_xf.sheet_names:
                vlm_df = vlm_xf.parse('VLM')
                print(f"VLM loaded: {len(vlm_df)} rows")
        except Exception as e:
            print(f"Failed to read VLM: {e}")

    # --- Match domain variables ---
    domain_results: List[Dict] = []
    if domains_df is not None and annots_df is not None:
        for _, row in domains_df.iterrows():
            domain = row['_domain']
            variable_name = str(row['_var_name'])
            supp_flag = str(row['_supp'])
            if not variable_name or variable_name == 'nan':
                continue
            matches = find_domain_variable_in_annotations(domain, variable_name, supp_flag, annots_df)
            for m in matches:
                domain_results.append({
                    'Type': 'Domain_Variable',
                    'Dataset': domain,
                    'Variable': variable_name,
                    'SUPP': supp_flag,
                    'Where_Clause': '',
                    'Parsed_Variable': variable_name,
                    'Parsed_Value': '',
                    'Found_Page': m['page'],
                    'Found_Content': m['content'],
                })

    # --- Match VLM Where Clauses ---
    vlm_results: List[Dict] = []
    if vlm_df is not None and annots_df is not None:
        wc_col = _find_col(vlm_df, ['Where Clause', 'Where_Clause', 'where_clause'])
        ds_col = _find_col(vlm_df, ['Dataset', 'dataset'])
        var_col = _find_col(vlm_df, ['Variable', 'variable'])
        if wc_col:
            for _, row in vlm_df.iterrows():
                where_clause = row.get(wc_col, '')
                dataset = row.get(ds_col, '') if ds_col else ''
                variable_name = row.get(var_col, '') if var_col else ''
                var, val = parse_where_clause(where_clause)
                if var and val:
                    matches = find_variable_in_annotations(var, val, annots_df)
                    for m in matches:
                        vlm_results.append({
                            'Type': 'VLM_WhereClause',
                            'Dataset': dataset,
                            'Variable': variable_name,
                            'SUPP': '',
                            'Where_Clause': where_clause,
                            'Parsed_Variable': var,
                            'Parsed_Value': val,
                            'Found_Page': m['page'],
                            'Found_Content': m['content'],
                        })

    all_results = domain_results + vlm_results
    results_df = pd.DataFrame(all_results) if all_results else pd.DataFrame(
        columns=['Type', 'Dataset', 'Variable', 'SUPP', 'Where_Clause',
                 'Parsed_Variable', 'Parsed_Value', 'Found_Page', 'Found_Content'])

    # Deduplicate: keep first page per (Dataset, Variable)
    if not results_df.empty:
        results_df = results_df.sort_values('Found_Page')

    if output_path and not results_df.empty:
        out = Path(output_path)
        out.parent.mkdir(parents=True, exist_ok=True)
        results_df.to_excel(str(out), index=False)
        print(f"Page mapping saved to: {out}")

    print(f"Pages extraction complete: {len(domain_results)} domain matches, {len(vlm_results)} VLM matches")
    return results_df


def build_pages_summary(results_df: pd.DataFrame) -> pd.DataFrame:
    """
    Collapse the detailed results into a summary table suitable for
    the sas_pages_data DB table.
    - Domain_Variable entries: one row per (Dataset, Variable), where_clause=''.
    - VLM_WhereClause entries: one row per (Dataset, Variable, Where_Clause),
      preserving each where clause separately.
    """
    if results_df.empty:
        return pd.DataFrame(columns=['dataset', 'variable', 'where_clause', 'pages', 'origin'])

    df = results_df.copy()
    df = df[~df['Dataset'].str.upper().str.startswith('SUPP')]
    df['Found_Page'] = df['Found_Page'].astype(str)

    domain_df = df[df['Type'] == 'Domain_Variable'].copy()
    vlm_df = df[df['Type'] == 'VLM_WhereClause'].copy()

    parts = []

    if not domain_df.empty:
        dg = domain_df.groupby(['Dataset', 'Variable']).agg(
            pages=('Found_Page', lambda x: ', '.join(sorted(set(x), key=lambda p: (int(p) if p.isdigit() else 9999)))),
            origin=('Type', 'first'),
        ).reset_index()
        dg['where_clause'] = ''
        dg.columns = ['dataset', 'variable', 'pages', 'origin', 'where_clause']
        parts.append(dg)

    if not vlm_df.empty:
        vg = vlm_df.groupby(['Dataset', 'Variable', 'Where_Clause']).agg(
            pages=('Found_Page', lambda x: ', '.join(sorted(set(x), key=lambda p: (int(p) if p.isdigit() else 9999)))),
            origin=('Type', 'first'),
        ).reset_index()
        vg.columns = ['dataset', 'variable', 'where_clause', 'pages', 'origin']
        parts.append(vg)

    if parts:
        result = pd.concat(parts, ignore_index=True)
    else:
        result = pd.DataFrame(columns=['dataset', 'variable', 'where_clause', 'pages', 'origin'])

    result = result[['dataset', 'variable', 'where_clause', 'pages', 'origin']]
    return result


if __name__ == "__main__":
    upload_base = os.environ.get('UPLOAD_BASE_PATH', r'C:\Project_Web\019_defineXML\uploads')
    project_id = os.environ.get('PROJECT_ID', 'default')
    python_base = os.environ.get('PYTHON_BASE_PATH', r'C:\Project_Web\019_defineXML\Python')

    annots = os.environ.get('ANNOTS_PATH', os.path.join(upload_base, project_id, 'output', 'Annots2.xlsx'))
    spec = os.environ.get('SPEC_PATH', '')
    vlm = os.environ.get('VLM_PATH', os.path.join(upload_base, project_id, 'output', 'vlm_codelists.xlsx'))
    out = os.environ.get('PAGES_OUTPUT_PATH', os.path.join(upload_base, project_id, 'output', 'variable_page_mapping.xlsx'))

    if not spec:
        synced = os.path.join(upload_base, project_id, 'output', f'spec_synced_{project_id}.xlsx')
        if os.path.isfile(synced):
            spec = synced
            print(f"[spec] 使用数据库同步文件: {synced}")
        else:
            spec_dir = os.path.join(upload_base, project_id, 'project-spec')
            if os.path.isdir(spec_dir):
                for f in os.listdir(spec_dir):
                    if f.lower().endswith(('.xlsx', '.xls')):
                        spec = os.path.join(spec_dir, f)
                        break
            if not spec:
                spec = os.path.join(python_base, 'define', '项目Spec', 'spec.xlsx')

    print(f"Annots path: {annots}")
    print(f"Spec path:   {spec}")
    print(f"VLM path:    {vlm}")
    print(f"Output path: {out}")

    results = extract_pages(annots, spec, vlm, out)
    summary = build_pages_summary(results)
    print(f"\nSummary: {len(summary)} variable-page entries")
    if not summary.empty:
        print(summary.head(10))
