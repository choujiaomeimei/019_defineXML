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
                 db_config: Dict[str, str] = None, project_id: str = "default"):
        """
        Initialize the extractor.
        
        Args:
            data_path: Path to directory containing XPT files
            spec_path: Path to spec.xlsx file
            output_path: Path for output files (default: same as spec_path)
            db_config: Database configuration for storing data (optional)
            project_id: Project ID for database storage
        """
        self.data_path = Path(data_path)
        self.spec_path = Path(spec_path)
        self.output_path = Path(output_path) if output_path else self.spec_path.parent
        self.db_config = db_config
        self.project_id = project_id
        self.db_manager = None
        
        # 如果提供了数据库配置，初始化数据库管理器
        if db_config and VLMCodeListDBManager:
            self.db_manager = VLMCodeListDBManager(db_config, project_id)
        
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
        """Read XPT file and return DataFrame."""
        file_path = self.data_path / f"{filename}.xpt"
        if file_path.exists():
            try:
                df = pd.read_sas(str(file_path), format='xport')
                # Clean string encoding issues
                for col in df.columns:
                    if df[col].dtype == 'object':
                        df[col] = df[col].apply(self.clean_text_encoding)
                return df
            except Exception as e:
                print(f"Error reading {filename}.xpt: {e}")
                return pd.DataFrame()
        else:
            print(f"File {filename}.xpt not found")
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
                vlm_record['Method'] = 'Y'
                
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
    
    def process_vars_by_spec(self, domain: str):
        """
        Process variables based on spec.xlsx for given domain.
        Equivalent to SAS macro %process_vars_byspec.
        """
        try:
            # Read spec file for the domain
            spec_df = pd.read_excel(self.spec_path, sheet_name=domain)
            
            # Filter relevant variables
            spec_df = spec_df[
                spec_df['CDISC_Submission_Value'].notna() &
                ~spec_df['Variable_Name'].str.contains('CATN', na=False) &
                ~spec_df['Variable_Name'].str.contains('TESTN', na=False)
            ]
            
            # Prepare variable list structure
            var_list = []
            for _, row in spec_df.iterrows():
                var_info = {
                    'dataset': row['Domain'],
                    'dsn': f"{row['Domain'].upper()}ALL",
                    'var': row['Variable_Name'],
                    'vlabel': row['Variable_Label'],
                    'vcd': row['CDISC_Submission_Value'],
                    'type': 'Char'  # Default, would need more logic to determine actual type
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
        # Get list of available XPT files
        xpt_files = list(self.data_path.glob("*ALL.xpt"))
        
        for xpt_file in xpt_files:
            domain = xpt_file.stem.replace('ALL', '')
            var_list = self.process_vars_by_spec(domain)
            
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
    
    def extract_vlm(self):
        """Extract VLM (Variable Level Metadata)."""
        print("Extracting VLM...")
        
        # Main function calls equivalent to SAS script
        self.export_unique_lbdata_byseq('ts', 'TSSEQ', ['TSPARMCD', 'TSPARM'], 'TSPARM')
        self.export_unique_lbdata_byseq('tx', 'TXSEQ', ['TXPARMCD', 'TXPARM'], 'TSPARM')
        self.export_unique_lbdata_byseq('ts', 'TSSEQ', ['TSPARMCD', 'TSPARM'])
        self.export_unique_lbdata_byseq('lb', 'LBSEQ', ['LBCAT', 'LBTESTCD', 'LBTEST'])
        self.export_unique_lbdata_byseq('vs', 'VSSEQ', ['VSTESTCD', 'VSTEST'])
        self.export_unique_lbdata_byseq('pe', 'PESEQ', ['PETESTCD', 'PETEST'])
        self.export_unique_lbdata_byseq('pc', 'PCSEQ', ['PCTESTCD', 'PCTEST'])
        self.export_unique_lbdata_byseq('pp', 'PPSEQ', ['PPTESTCD', 'PPTEST'], 'PKPARM')
        self.export_unique_lbdata_byseq('eg', 'EGSEQ', ['EGTESTCD', 'EGTEST'])
        
        self.get_unique_cd('dm', 'SUBJID', 'ARM', 'ARMCD')
        self.get_unique_cd('tx', 'TXSEQ', 'SET', 'SETCD')
        self.get_unique_cd('ta', 'TAETORD', 'ELEMENT', 'ETCD')
        
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
        target_spec_file = Path(r"E:\JAVAPROJ\008_defineXML\Python\output\Spec_out\spec.xlsx")
        
        # Save to output directory
        with pd.ExcelWriter(output_file, engine='openpyxl') as writer:
            vlm_data.to_excel(writer, sheet_name='VLM', index=False)
            codelist_data.to_excel(writer, sheet_name='codelists', index=False)
        
        # Save to target spec.xlsx
        try:
            # Read existing sheets from target spec file
            existing_sheets = {}
            if target_spec_file.exists():
                with pd.ExcelFile(target_spec_file) as xls:
                    for sheet_name in xls.sheet_names:
                        if sheet_name not in ['VLM', 'codelists']:
                            existing_sheets[sheet_name] = pd.read_excel(xls, sheet_name=sheet_name)
            
            # Write all sheets including new ones to target spec file
            with pd.ExcelWriter(target_spec_file, engine='openpyxl') as writer:
                # Write existing sheets first
                for sheet_name, data in existing_sheets.items():
                    data.to_excel(writer, sheet_name=sheet_name, index=False)
                
                # Write VLM and codelists
                vlm_data.to_excel(writer, sheet_name='VLM', index=False)
                codelist_data.to_excel(writer, sheet_name='codelists', index=False)
                
        except Exception as e:
            print(f"Error updating target spec.xlsx: {e}")
            # Fallback: create new file with just VLM and codelists
            with pd.ExcelWriter(target_spec_file, engine='openpyxl') as writer:
                vlm_data.to_excel(writer, sheet_name='VLM', index=False)
                codelist_data.to_excel(writer, sheet_name='codelists', index=False)
        
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
    
    # Configuration - adjust paths as needed
    data_path = r"E:\JAVAPROJ\008_defineXML\Python\define\sdtm define package"  # Path to XPT files
    spec_path = r"E:\JAVAPROJ\008_defineXML\Python\define\项目Spec\spec.xlsx"  # Path to spec.xlsx
    output_path = r"E:\JAVAPROJ\008_defineXML\Python\define\项目Spec\out"  # Output directory
    
    # Database configuration (optional, uncomment and modify if needed)
    db_config = {
        'host': 'localhost',
        'user': 'root',
        'password': 'root',  # 请修改为实际密码
        'database': 'define_db'  # 请修改为实际数据库名
    }
    
    # 从环境变量获取项目ID，如果没有则使用默认值
    project_id = os.environ.get('PROJECT_ID', 'MJR-MR001-01')
    print(f"使用项目ID: {project_id}")
    
    # Create output directory if it doesn't exist
    Path(output_path).mkdir(parents=True, exist_ok=True)
    
    # Initialize and run extractor (with database support)
    try:
        extractor = VLMCodeListExtractor(data_path, spec_path, output_path, db_config, project_id)
        vlm_data, codelist_data = extractor.run()
    except Exception as e:
        print(f"使用数据库配置失败: {e}")
        print("回退到仅Excel输出模式...")
        # Fallback to Excel-only mode
        extractor = VLMCodeListExtractor(data_path, spec_path, output_path)
        vlm_data, codelist_data = extractor.run()
    
    return vlm_data, codelist_data


if __name__ == "__main__":
    main()