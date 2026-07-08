#!/usr/bin/env python3
"""
VLM and CodeList Extractor
Based on SAS script logic to extract VLM and CodeList from XPT files and project spec Excel file.
"""

import pandas as pd
import numpy as np
from pathlib import Path
import os
from typing import Dict, List, Tuple, Any
import warnings
import sys

# 添加当前目录到Python路径
sys.path.append(str(Path(__file__).parent))

# 导入数据库管理器
try:
    from vlm_codelist_db_manager import VLMCodeListDBManager
except ImportError:
    VLMCodeListDBManager = None
    print("警告: 无法导入数据库管理器，仅支持Excel输出")

warnings.filterwarnings('ignore')


class VLMCodeListExtractor:
    """Extract VLM (Variable Level Metadata) and CodeList from SDTM data and spec files."""
    
    def __init__(self, data_path: str, spec_path: str, output_path: str = None, 
                 db_config: Dict[str, str] = None, project_id: str = "default",
                 username: str = None):
        """
        Initialize the extractor.
        
        Args:
            data_path: Path to directory containing XPT files
            spec_path: Path to spec.xlsx file
            output_path: Path for output files (default: same as spec_path)
            db_config: Database configuration for storing data (optional)
            project_id: Project ID for database storage
            username: Username for data isolation
        """
        self.data_path = Path(data_path)
        self.spec_path = Path(spec_path)
        self.output_path = Path(output_path) if output_path else self.spec_path.parent
        self.db_config = db_config
        self.project_id = project_id
        self.username = username or os.environ.get('USERNAME_CONTEXT', '')
        self.db_manager = None
        
        self._spec_xf = pd.ExcelFile(self.spec_path)
        self._spec_sheet_map = {s.lower(): s for s in self._spec_xf.sheet_names}
        
        if db_config and VLMCodeListDBManager:
            self.db_manager = VLMCodeListDBManager(db_config, project_id, username=self.username)
        
        # Initialize output structures - match SAS column names exactly
        self.final_vlm = pd.DataFrame(columns=[
            'Dataset', 'Variable', 'Where Clause', 'Label', 'Controlled Terms or Format', 
            'Origin', 'Pages', 'Derivation/Comment', 'Method', 'Comment', '类别'
        ])
        
        self.final_code_test = pd.DataFrame(columns=[
            'vcd', 'vlabel', 'type', 'cdnum', 'code', 'codeDes', 'codever'
        ])
        
        self.all_results = pd.DataFrame()
        
    def clean_text_encoding(self, text):
        """Clean text encoding issues from XPT files."""
        if pd.isna(text) or text is None:
            return ''
        
        text = str(text)
        
        if text == 'nan':
            return ''
            
        # Handle byte strings
        if text.startswith("b'") and text.endswith("'"):
            text = text[2:-1]
            
        # Try to decode if it contains escape sequences
        if '\\x' in text:
            try:
                # Convert escape sequences to bytes then decode as UTF-8
                import codecs
                decoded = codecs.decode(text, 'unicode_escape')
                if isinstance(decoded, bytes):
                    text = decoded.decode('utf-8')
                else:
                    text = decoded
            except Exception as e:
                # If decoding fails, try latin1 then utf-8
                try:
                    text = text.encode('latin1').decode('utf-8')
                except:
                    pass  # Keep original text if all decoding attempts fail
                    
        # Additional cleanup for common encoding issues
        try:
            # If text looks like it was double-encoded, try to fix it
            if isinstance(text, str):
                # Try to detect and fix UTF-8 that was interpreted as latin1
                test_encoded = text.encode('latin1')
                text = test_encoded.decode('utf-8')
        except:
            pass  # Keep original if conversion fails
            
        return text
    
    def read_xpt_file(self, filename: str) -> pd.DataFrame:
        """Read XPT file and return DataFrame. Handles UUID-prefixed filenames."""
        file_path = self.data_path / f"{filename}.xpt"
        if not file_path.exists():
            # uploaded files may have uuid prefix: {uuid}_{filename}.xpt
            matches = list(self.data_path.glob(f"*_{filename}.xpt"))
            if matches:
                file_path = matches[0]
            else:
                # case-insensitive fallback
                matches = [p for p in self.data_path.glob("*.xpt")
                           if p.stem.lower().endswith(f"_{filename.lower()}")
                           or p.stem.lower() == filename.lower()]
                if matches:
                    file_path = matches[0]
        if file_path.exists():
            try:
                df = pd.read_sas(str(file_path), format='xport')
                for col in df.columns:
                    if df[col].dtype == 'object':
                        df[col] = df[col].apply(self.clean_text_encoding)
                return df
            except Exception as e:
                print(f"Error reading {filename}.xpt: {e}")
                return pd.DataFrame()
        else:
            print(f"File {filename}.xpt not found in {self.data_path}")
            return pd.DataFrame()
    
    def export_unique_lbdata_byseq(self, dataset: str, byvar: str, vars_list: List[str], cd: str = None):
        """
        Extract unique data based on sequence variables.
        Equivalent to SAS macro %export_unique_lbdata_byseq.
        """
        df = self.read_xpt_file(dataset)
        if df.empty:
            return
            
        # Convert column names to uppercase for consistency
        df.columns = df.columns.str.upper()
        vars_list = [v.upper() for v in vars_list]
        byvar = byvar.upper()
        
        # Sort by byvar
        if byvar in df.columns:
            df = df.sort_values(byvar)
        
        # Remove duplicates based on vars_list
        unique_data = df.drop_duplicates(subset=vars_list)
        
        # Process each unique record
        vlm_records = []
        code_records = []
        
        for idx, row in unique_data.iterrows():
            # Create VLM record - match SAS column names exactly
            vlm_record = {
                'Dataset': dataset.upper(),
                'Variable': '',
                'Where Clause': '',
                'Label': '',
                'Controlled Terms or Format': '',
                'Origin': '',
                'Pages': '',
                'Derivation/Comment': '',
                'Method': '',
                'Comment': '',
                '类别': ''
            }
            
            # Handle different variable patterns
            if 'TSPARMCD' in vars_list:
                vlm_record['Variable'] = f"{dataset.upper()}VAL"
                vlm_record['Where Clause'] = f"{dataset.upper()}PARMCD EQ \"{row.get('TSPARMCD', '')}\""
                vlm_record['Label'] = row.get('TSPARM', '')
                vlm_record['Origin'] = 'Assigned'
                vlm_record['Derivation/Comment'] = str(row.get('TSVAL', ''))
                
            elif any(col.endswith('PARMCD') for col in vars_list if col != 'TSPARMCD'):
                # Handle other PARMCD patterns like TXPARMCD
                parmcd_col = next(col for col in vars_list if col.endswith('PARMCD') and col != 'TSPARMCD')
                parm_col = parmcd_col.replace('CD', '')  # TXPARMCD -> TXPARM
                vlm_record['Variable'] = f"{dataset.upper()}VAL"
                vlm_record['Where Clause'] = f"{dataset.upper()}PARMCD EQ \"{row.get(parmcd_col, '')}\""
                vlm_record['Label'] = row.get(parm_col, '')
                vlm_record['Origin'] = 'Assigned'
                
            elif any(col.endswith('TESTCD') for col in vars_list):
                # Handle TESTCD patterns like LBTESTCD, VSTESTCD, etc.
                testcd_col = next(col for col in vars_list if col.endswith('TESTCD'))
                test_col = testcd_col.replace('CD', '')  # LBTESTCD -> LBTEST
                vlm_record['Variable'] = f"{dataset.upper()}ORRES"
                vlm_record['Where Clause'] = f"{dataset.upper()}TESTCD EQ \"{row.get(testcd_col, '')}\""
                vlm_record['Label'] = row.get(test_col, '')
                vlm_record['Origin'] = 'CRF'
            
            vlm_records.append(vlm_record)
            
            # Create CodeList records
            if any(col.endswith('TESTCD') for col in vars_list):
                # Handle TESTCD patterns like LBTESTCD, VSTESTCD, etc.
                testcd_col = next(col for col in vars_list if col.endswith('TESTCD'))
                test_col = testcd_col.replace('CD', '')  # LBTESTCD -> LBTEST
                
                # TESTCD record
                code_record1 = {
                    'vcd': f"{dataset.upper()}TESTCD",
                    'vlabel': f"{dataset.upper()} Test or Examination Short Name",
                    'type': 'Char',
                    'cdnum': str(row.get(byvar, idx + 1)),
                    'code': row.get(testcd_col, ''),
                    'codeDes': row.get(test_col, ''),
                    'codever': ''
                }
                
                # TEST record
                code_record2 = {
                    'vcd': f"{dataset.upper()}TEST",
                    'vlabel': f"{dataset.upper()} Test or Examination",
                    'type': 'Char',
                    'cdnum': str(row.get(byvar, idx + 1)),
                    'code': row.get(test_col, ''),
                    'codeDes': '',
                    'codever': ''
                }
                
                code_records.extend([code_record1, code_record2])
                
            elif any(col.endswith('PARMCD') for col in vars_list if col != 'TSPARMCD'):
                # Handle other PARMCD patterns like TXPARMCD
                parmcd_col = next(col for col in vars_list if col.endswith('PARMCD') and col != 'TSPARMCD')
                parm_col = parmcd_col.replace('CD', '')  # TXPARMCD -> TXPARM
                
                # PARMCD record
                code_record1 = {
                    'vcd': f"{dataset.upper()}PARMCD",
                    'vlabel': f"{dataset.upper()} Parameter Short Name",
                    'type': 'Char',
                    'cdnum': str(idx + 1),
                    'code': row.get(parmcd_col, ''),
                    'codeDes': row.get(parm_col, ''),
                    'codever': ''
                }
                
                # PARM record
                code_record2 = {
                    'vcd': f"{dataset.upper()}PARM",
                    'vlabel': f"{dataset.upper()} Parameter",
                    'type': 'Char',
                    'cdnum': str(idx + 1),
                    'code': row.get(parm_col, ''),
                    'codeDes': '',
                    'codever': ''
                }
                
                code_records.extend([code_record1, code_record2])
        
        # Apply cd parameter if provided
        if cd:
            for record in code_records:
                if record['vcd'].endswith('CD'):
                    record['vcd'] = f"{cd}CD"
                else:
                    record['vcd'] = cd
        
        # Append to final datasets
        if vlm_records:
            vlm_df = pd.DataFrame(vlm_records)
            self.final_vlm = pd.concat([self.final_vlm, vlm_df], ignore_index=True)
            
        if code_records:
            code_df = pd.DataFrame(code_records)
            self.final_code_test = pd.concat([self.final_code_test, code_df], ignore_index=True)
    
    def get_unique_cd(self, dataset: str, byvar: str, var: str, varcd: str):
        """
        Get unique coded values.
        Equivalent to SAS macro %get_unique_cd.
        """
        df = self.read_xpt_file(dataset)
        if df.empty:
            return
            
        # Convert column names to uppercase
        df.columns = df.columns.str.upper()
        var = var.upper()
        varcd = varcd.upper()
        byvar = byvar.upper()
        
        # Sort by byvar
        if byvar in df.columns:
            df = df.sort_values(byvar)
        
        # Get unique values for var
        unique_values = df[var].drop_duplicates()
        
        code_records = []
        for i, value in enumerate(unique_values, 1):
            # Get corresponding varcd value
            varcd_value = df[df[var] == value][varcd].iloc[0] if varcd in df.columns else value
            
            # varcd record
            code_record1 = {
                'vcd': varcd,
                'vlabel': f"{varcd} Label",  # Would need actual label from metadata
                'type': 'Char',
                'cdnum': str(i),
                'code': str(varcd_value),
                'codeDes': str(value),
                'codever': ''
            }
            
            # var record
            code_record2 = {
                'vcd': var,
                'vlabel': f"{var} Label",  # Would need actual label from metadata
                'type': 'Char',
                'cdnum': str(i),
                'code': str(value),
                'codeDes': '',
                'codever': ''
            }
            
            code_records.extend([code_record1, code_record2])
        
        if code_records:
            code_df = pd.DataFrame(code_records)
            self.final_code_test = pd.concat([self.final_code_test, code_df], ignore_index=True)
    
    def _normalize_column(self, col: str) -> str:
        """Normalize a column name to a canonical key for matching."""
        return col.strip().lower().replace(' ', '_').replace('-', '_')

    def _find_column(self, df: pd.DataFrame, aliases: list) -> str:
        """Find the first matching column from a list of aliases (case/space insensitive)."""
        norm_map = {self._normalize_column(c): c for c in df.columns}
        for alias in aliases:
            key = self._normalize_column(alias)
            if key in norm_map:
                return norm_map[key]
        return None

    def process_vars_by_spec(self, domain: str):
        """
        Process variables based on spec.xlsx for given domain.
        Equivalent to SAS macro %process_vars_byspec.
        """
        try:
            actual_sheet = self._spec_sheet_map.get(domain.lower())
            if actual_sheet is None:
                return []

            spec_df = self._spec_xf.parse(actual_sheet)

            col_cdisc = self._find_column(spec_df, [
                'CDISC_Submission_Value', 'CDISC Submission Value',
                'Controlled Terms or Format', 'Codelist/Controlled Terms',
                'Codelist', 'CT', 'controlled_terms_or_format'
            ])
            col_varname = self._find_column(spec_df, [
                'Variable_Name', 'Variable Name', 'variable', 'Variable',
                'VARIABLE', 'var_name', 'varname'
            ])
            col_varlabel = self._find_column(spec_df, [
                'Variable_Label', 'Variable Label', 'Label', 'label',
                'LABEL', 'var_label', 'Description'
            ])
            col_domain = self._find_column(spec_df, [
                'Domain', 'domain', 'DOMAIN', 'Dataset'
            ])

            if col_varname is None:
                print(f"  Sheet '{actual_sheet}' has no recognizable Variable Name column – skipping")
                return []

            if col_cdisc is not None:
                spec_df = spec_df[spec_df[col_cdisc].notna()]

            if col_varname:
                spec_df = spec_df[
                    ~spec_df[col_varname].astype(str).str.contains('CATN', na=False) &
                    ~spec_df[col_varname].astype(str).str.contains('TESTN', na=False)
                ]

            var_list = []
            for _, row in spec_df.iterrows():
                d = str(row[col_domain]).upper() if col_domain and pd.notna(row.get(col_domain)) else domain.upper()
                var_info = {
                    'dataset': d,
                    'dsn': f"{d}ALL",
                    'var': str(row[col_varname]) if col_varname else '',
                    'vlabel': str(row[col_varlabel]) if col_varlabel and pd.notna(row.get(col_varlabel)) else '',
                    'vcd': str(row[col_cdisc]) if col_cdisc and pd.notna(row.get(col_cdisc)) else '',
                    'type': 'Char'
                }
                var_list.append(var_info)

            return var_list

        except Exception as e:
            print(f"Error processing spec for domain {domain}: {e}")
            return []
    
    def collect_values(self, dsn: str, var: str, var_info: dict):
        """
        Collect unique values for a variable from dataset.
        Equivalent to SAS macro %collect_values.
        """
        df = self.read_xpt_file(dsn.replace('ALL', ''))
        if df.empty:
            return
            
        # Convert column names to uppercase
        df.columns = df.columns.str.upper()
        var = var.upper()
        
        if var not in df.columns:
            return
            
        # Get unique values
        unique_values = df[var].dropna().unique()
        codelist = '#'.join([str(v) for v in sorted(unique_values)])
        
        result = {
            'dsn': dsn,
            'var': var,
            'vlabel': var_info.get('vlabel', ''),
            'type': var_info.get('type', 'Char'),
            'vcd': var_info.get('vcd', ''),
            'codelist': codelist
        }
        
        result_df = pd.DataFrame([result])
        self.all_results = pd.concat([self.all_results, result_df], ignore_index=True)
    
    def auto_cd(self):
        """
        Automatically generate codecd values.
        Equivalent to SAS macro %auto_cd.
        """
        if self.all_results.empty:
            return
            
        # Filter non-empty codelists
        results = self.all_results[self.all_results['codelist'].notna() & (self.all_results['codelist'] != '')]
        
        # Generate codecd
        results_with_cd = []
        for var_group in results.groupby('var'):
            var_name = var_group[0]
            var_data = var_group[1].reset_index(drop=True)
            
            for idx, row in var_data.iterrows():
                if idx == 0:
                    codecd = var_name
                else:
                    codecd = f"{var_name}_{idx}"
                
                row_dict = row.to_dict()
                row_dict['codecd'] = codecd
                results_with_cd.append(row_dict)
        
        self.all_results = pd.DataFrame(results_with_cd)
    
    def export_var_values(self):
        """
        Export variable values from all datasets.
        Equivalent to SAS macro %export_var_values.
        """
        xpt_files = list(self.data_path.glob("*.xpt"))

        seen_domains = set()
        for xpt_file in xpt_files:
            stem = xpt_file.stem.lower()
            # handle uuid-prefixed names: {uuid}_{domain}.xpt
            if '_' in stem:
                domain = stem.rsplit('_', 1)[-1]
            else:
                domain = stem
            domain = domain.replace('all', '')
            if not domain or domain in seen_domains:
                continue
            seen_domains.add(domain)

            var_list = self.process_vars_by_spec(domain.upper())
            for var_info in var_list:
                self.collect_values(var_info['dsn'], var_info['var'], var_info)
        
        # Apply auto_cd
        self.auto_cd()
        
        # Split codelist into individual codes
        split_results = []
        for _, row in self.all_results.iterrows():
            if pd.isna(row['codelist']) or row['codelist'] == '':
                continue
                
            codes = row['codelist'].split('#')
            for i, code in enumerate(codes, 1):
                split_record = {
                    'vcd': row['vcd'],
                    'vlabel': row['vlabel'],
                    'type': row['type'],
                    'cdnum': i,
                    'code': code,
                    'codeDes': '',
                    'codever': ''
                }
                split_results.append(split_record)
        
        return pd.DataFrame(split_results)
    
    def _available_domains(self) -> set:
        """Return a set of domain names available as XPT files (lowercase)."""
        domains = set()
        for f in self.data_path.glob("*.xpt"):
            stem = f.stem.lower()
            if '_' in stem:
                stem = stem.rsplit('_', 1)[-1]
            domains.add(stem)
        return domains

    def extract_vlm(self):
        """Extract VLM (Variable Level Metadata)."""
        print("Extracting VLM...")
        avail = self._available_domains()

        vlm_configs = [
            ('ts', 'TSSEQ', ['TSPARMCD', 'TSPARM'], 'TSPARM'),
            ('tx', 'TXSEQ', ['TXPARMCD', 'TXPARM'], 'TSPARM'),
            ('lb', 'LBSEQ', ['LBCAT', 'LBTESTCD', 'LBTEST'], None),
            ('vs', 'VSSEQ', ['VSTESTCD', 'VSTEST'], None),
            ('pe', 'PESEQ', ['PETESTCD', 'PETEST'], None),
            ('pc', 'PCSEQ', ['PCTESTCD', 'PCTEST'], None),
            ('pp', 'PPSEQ', ['PPTESTCD', 'PPTEST'], 'PKPARM'),
            ('eg', 'EGSEQ', ['EGTESTCD', 'EGTEST'], None),
            ('fa', 'FASEQ', ['FATESTCD', 'FATEST'], None),
            ('da', 'DASEQ', ['DATESTCD', 'DATEST'], None),
            ('mb', 'MBSEQ', ['MBTESTCD', 'MBTEST'], None),
            ('mi', 'MISEQ', ['MITESTCD', 'MITEST'], None),
            ('ms', 'MSSEQ', ['MSTESTCD', 'MSTEST'], None),
            ('tu', 'TUSEQ', ['TUTESTCD', 'TUTEST'], None),
            ('tr', 'TRSEQ', ['TRTESTCD', 'TRTEST'], None),
            ('rs', 'RSSEQ', ['RSTESTCD', 'RSTEST'], None),
        ]

        for cfg in vlm_configs:
            ds = cfg[0]
            if ds in avail:
                self.export_unique_lbdata_byseq(ds, cfg[1], cfg[2], cfg[3])
        
        cd_configs = [
            ('dm', 'SUBJID', 'ARM', 'ARMCD'),
            ('tx', 'TXSEQ', 'SET', 'SETCD'),
            ('ta', 'TAETORD', 'ELEMENT', 'ETCD'),
        ]
        for ds, byv, var, varcd in cd_configs:
            if ds in avail:
                self.get_unique_cd(ds, byv, var, varcd)
        
        # Set labels for final_vlm - match SAS column order exactly
        self.final_vlm = self.final_vlm[[
            'Dataset', 'Variable', 'Where Clause', 'Label', 'Controlled Terms or Format', 
            'Origin', 'Pages', 'Derivation/Comment', 'Method', 'Comment', '类别'
        ]]
        
        print(f"VLM extraction completed. {len(self.final_vlm)} records extracted.")
        return self.final_vlm
    
    def extract_codelist(self):
        """Extract CodeList information."""
        print("Extracting CodeList...")
        
        # Export variable values
        split_codelist = self.export_var_values()
        
        # Merge with final_code_test
        if not split_codelist.empty and not self.final_code_test.empty:
            # Sort and renumber final_code_test
            self.final_code_test = self.final_code_test.sort_values(['vcd', 'cdnum']).reset_index(drop=True)
            
            # Renumber cdnum within each vcd group
            for vcd_group in self.final_code_test.groupby('vcd'):
                vcd_name = vcd_group[0]
                group_data = vcd_group[1]
                
                # Update cdnum sequentially
                self.final_code_test.loc[
                    self.final_code_test['vcd'] == vcd_name, 'cdnum'
                ] = range(1, len(group_data) + 1)
            
            # For numeric types with non-empty codeDes, use sequential codes
            numeric_mask = (self.final_code_test['type'] == 'Num') & (self.final_code_test['codeDes'] != '')
            self.final_code_test.loc[numeric_mask, 'code'] = self.final_code_test.loc[numeric_mask, 'cdnum'].astype(str)
            
            # Merge datasets
            codelists = pd.concat([split_codelist, self.final_code_test], ignore_index=True)
        else:
            codelists = split_codelist if not split_codelist.empty else self.final_code_test
        
        # Sort final result
        if not codelists.empty:
            codelists = codelists.sort_values(['vcd', 'cdnum', 'code']).reset_index(drop=True)
            
            # Add flag for specific variables
            flag_vars = [
                'ACTARM', 'ACTARMCD', 'ARM', 'ARMCD', 'PCTEST', 'PCTESTCD', 
                'PKPARM', 'PKPARMCD', 'ETCD', 'ELEMENT', 'VISIT', 'VISITNUM', 
                'IETEST', 'IETESTCD'
            ]
            codelists['flag'] = codelists['vcd'].apply(lambda x: 'Y' if x in flag_vars else '')
        
        print(f"CodeList extraction completed. {len(codelists)} records extracted.")
        return codelists
    
    def save_results(self, vlm_data: pd.DataFrame, codelist_data: pd.DataFrame):
        """Save results to Excel files and database."""
        output_file = self.output_path / "vlm_codelists.xlsx"
        target_spec_file = self.output_path / "spec_with_vlm.xlsx"

        # Save to output directory
        with pd.ExcelWriter(output_file, engine='openpyxl') as writer:
            vlm_data.to_excel(writer, sheet_name='VLM', index=False)
            codelist_data.to_excel(writer, sheet_name='codelists', index=False)

        try:
            import sys as _sys, os as _os
            _sys.path.insert(0, _os.path.join(_os.path.dirname(__file__), '..', 'pgm'))
            from excel_style import style_excel_file
            style_excel_file(output_file)
        except Exception as style_err:
            print(f"excel_style unavailable, skipping styling: {style_err}")

        # Merge VLM/codelists into a copy of the project spec
        try:
            existing_sheets = {}
            if self.spec_path.exists():
                for sheet_name in self._spec_xf.sheet_names:
                    if sheet_name not in ['VLM', 'codelists']:
                        existing_sheets[sheet_name] = self._spec_xf.parse(sheet_name)

            with pd.ExcelWriter(target_spec_file, engine='openpyxl') as writer:
                for sheet_name, data in existing_sheets.items():
                    data.to_excel(writer, sheet_name=sheet_name, index=False)
                vlm_data.to_excel(writer, sheet_name='VLM', index=False)
                codelist_data.to_excel(writer, sheet_name='codelists', index=False)

            try:
                style_excel_file(target_spec_file)
            except Exception:
                pass

        except Exception as e:
            print(f"Error creating spec_with_vlm.xlsx: {e}")

        print(f"Results saved to:")
        print(f"  - {output_file}")
        print(f"  - {target_spec_file}")
        
        # Save to database if configured
        if self.db_manager:
            try:
                if self.db_manager.connect_db():
                    # Create tables if not exist
                    self.db_manager.create_tables_if_not_exists()
                    
                    # Clear old project data
                    self.db_manager.clear_project_data()
                    
                    # Import new data
                    self.db_manager.import_vlm_data(vlm_data, created_by="vlm_extractor")
                    self.db_manager.import_codelist_data(codelist_data, created_by="vlm_extractor")
                    
                    print(f"  - Database (Project: {self.project_id})")
                    
            except Exception as e:
                print(f"Database save failed: {e}")
            finally:
                if self.db_manager:
                    self.db_manager.close_db()
    
    def run(self):
        """Run the complete extraction process."""
        print("Starting VLM and CodeList extraction...")
        
        # Extract VLM first
        vlm_data = self.extract_vlm()
        
        # Extract CodeList
        codelist_data = self.extract_codelist()
        
        # Save results
        self.save_results(vlm_data, codelist_data)
        
        print("Extraction completed successfully!")
        return vlm_data, codelist_data


def main():
    """Main function to run the extractor."""
    import os

    base_python_path = os.environ.get('PYTHON_BASE_PATH', r'C:\Project_Web\019_defineXML\Python')
    upload_base_path = os.environ.get('UPLOAD_BASE_PATH', r'C:\Project_Web\019_defineXML\uploads')
    project_id = os.environ.get('PROJECT_ID', 'MJR-MR001-01')

    data_path = os.environ.get('DATA_PATH', os.path.join(upload_base_path, project_id, 'xpt'))
    spec_path = os.environ.get('SPEC_PATH', '')
    output_path = os.environ.get('OUTPUT_PATH', os.path.join(upload_base_path, project_id, 'output'))

    if not spec_path:
        synced = os.path.join(upload_base_path, project_id, 'output', f'spec_synced_{project_id}.xlsx')
        if os.path.isfile(synced):
            spec_path = synced
            print(f"[spec] 使用数据库同步文件: {synced}")
        else:
            spec_dir = os.path.join(upload_base_path, project_id, 'project-spec')
            if os.path.isdir(spec_dir):
                for f in os.listdir(spec_dir):
                    if f.lower().endswith('.xlsx') or f.lower().endswith('.xls'):
                        spec_path = os.path.join(spec_dir, f)
                        break
            if not spec_path:
                spec_path = os.path.join(base_python_path, 'define', '项目Spec', 'spec.xlsx')

    username = os.environ.get('USERNAME_CONTEXT', '')

    db_config = {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'user': os.environ.get('DB_USER', 'root'),
        'password': os.environ.get('DB_PASSWORD', '123123'),
        'database': os.environ.get('DB_NAME', 'define_db')
    }

    print(f"使用项目ID: {project_id}")
    print(f"XPT数据路径: {data_path}")
    print(f"Spec文件路径: {spec_path}")
    print(f"输出路径: {output_path}")
    print(f"用户名: {username or '(未指定)'}")

    Path(output_path).mkdir(parents=True, exist_ok=True)

    try:
        extractor = VLMCodeListExtractor(data_path, spec_path, output_path, db_config, project_id, username=username)
        vlm_data, codelist_data = extractor.run()
    except Exception as e:
        print(f"使用数据库配置失败: {e}")
        print("回退到仅Excel输出模式...")
        extractor = VLMCodeListExtractor(data_path, spec_path, output_path)
        vlm_data, codelist_data = extractor.run()

    return vlm_data, codelist_data


if __name__ == "__main__":
    main()