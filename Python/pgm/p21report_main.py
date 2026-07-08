#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
重构版P21报告生成器 - 模块化设计，每个sheet独立工作
参考complete_define_generator.py的结构
"""

import pandas as pd
import numpy as np
from pathlib import Path
from datetime import datetime
import sys
import re
import time
import json
import shutil
import argparse
from typing import Dict, List, Any, Optional

# 导入配置
sys.path.append(str(Path(__file__).parent))
from config import CONFIG

def read_config_from_args():
    """从命令行参数读取配置文件"""
    parser = argparse.ArgumentParser(description='P21报告生成器')
    parser.add_argument('--config', type=str, help='配置文件路径')
    parser.add_argument('--task-id', type=str, help='任务ID')

    args = parser.parse_args()

    if args.config:
        config_path = Path(args.config)
        if config_path.exists():
            with open(config_path, 'r', encoding='utf-8') as f:
                web_config = json.load(f)
            print(f"已读取配置文件: {config_path}")
            print(f"任务ID: {args.task_id}")

            # 将Web配置合并到默认配置中
            merged_config = CONFIG.copy()
            merged_config.update(web_config)
            return merged_config, args.task_id
        else:
            print(f"配置文件不存在: {config_path}")
            return CONFIG, None

    # 如果没有指定配置文件，使用默认配置
    print("使用默认配置")
    return CONFIG, None

# 内置工具函数
def read_excel_sheet(file_path: Path, sheet_name: str) -> pd.DataFrame:
    """读取Excel文件指定工作表"""
    try:
        return pd.read_excel(file_path, sheet_name=sheet_name)
    except Exception as e:
        print(f"读取Excel错误 {file_path} sheet {sheet_name}: {e}")
        return pd.DataFrame()

def create_dir(path: Path) -> None:
    """创建目录"""
    path.mkdir(parents=True, exist_ok=True)

def export_to_excel(df: pd.DataFrame, file_path: Path, sheet_name: str) -> None:
    """导出DataFrame到Excel文件指定工作表"""
    try:
        if not file_path.exists():
            with pd.ExcelWriter(file_path, engine='openpyxl') as writer:
                df.to_excel(writer, sheet_name=sheet_name, index=False)
        else:
            with pd.ExcelWriter(file_path, engine='openpyxl', mode='a', if_sheet_exists='replace') as writer:
                df.to_excel(writer, sheet_name=sheet_name, index=False)
        from excel_style import style_excel_file
        style_excel_file(file_path)
    except Exception as e:
        print(f"导出Excel错误 {file_path} sheet {sheet_name}: {e}")

class P21SheetProcessor:
    """P21各工作表处理器 - 每个sheet独立工作"""
    
    def __init__(self, config: dict, spec_data: dict, p21_data: dict):
        """初始化处理器"""
        self.config = config
        self.spec_data = spec_data
        self.p21_data = p21_data
        self.comments_data = []  # 存储Comments数据
        self.methods_data = []   # 存储Methods数据
        
    def parse_comment_method(self, text):
        """解析注释和方法文本，提取描述、文档和页码"""
        if pd.isna(text) or text == "":
            return {'description': '', 'document': '', 'pages': ''}
            
        text = str(text)
        
        # 解析{#document#pages}格式
        pattern = r'\{#([^#]+)#([^}]+)\}'
        match = re.search(pattern, text)
        
        if match:
            document = match.group(1)
            pages = match.group(2)
            description = re.sub(pattern, '', text).strip()
        else:
            description = text
            document = ''
            pages = ''
            
        return {
            'description': description,
            'document': document.lower() if document else '',
            'pages': pages
        }
    
    def get_variable_lengths_from_xpt(self, data_dir: Path) -> dict:
        """从XPT数据集获取变量的真实最大长度"""
        length_dict = {}
        
        try:
            import xport
            
            # 限制只分析几个主要数据集，避免处理时间过长
            main_datasets = ['DM', 'AE', 'VS', 'LB', 'EX', 'CM', 'MH', 'DS', 'IE', 'TS', 'PC', 'PP', 'PE']
            
            for dataset_name in main_datasets:
                xpt_path = data_dir / f"{dataset_name.lower()}.xpt"
                
                if xpt_path.exists():
                    try:
                        # 读取XPT文件
                        with open(xpt_path, 'rb') as f:
                            ds = xport.Dataset(f)
                            df = ds.to_dataframe()
                        
                        # 计算每个变量的最大长度
                        for col in df.columns:
                            if df[col].dtype == 'object':  # 字符变量
                                max_len = df[col].astype(str).str.len().max()
                                if pd.isna(max_len):
                                    max_len = 1
                            else:  # 数值变量
                                max_len = len(str(df[col].max())) if not pd.isna(df[col].max()) else 8
                            
                            length_dict[(dataset_name, col)] = max_len
                            
                    except Exception as e:
                        print(f"    警告：无法读取{xpt_path}: {e}")
                        
        except ImportError:
            print("    警告：未安装xport包，使用默认长度")
                    
        return length_dict
        
class DefineSheetProcessor(P21SheetProcessor):
    """Define工作表处理器"""
    
    def generate_define_data(self) -> pd.DataFrame:
        """生成Define表数据"""
        print("正在处理Define表单...")
        
        # 使用配置文件中的默认值
        protocol_id = self.config.get('PROTOCAL', 'MJR-MR001-01')
        study_title = self.config.get('STUDY_TITLE', 'MR001药物I期临床试验')
        
        # 更新Define信息
        define_df = self.p21_data['study'].copy()
        
        # 更新相关字段
        if protocol_id:
            define_df.loc[define_df['Attribute'] == 'StudyName', 'Value'] = protocol_id
            define_df.loc[define_df['Attribute'] == 'ProtocolName', 'Value'] = protocol_id
        if study_title:
            define_df.loc[define_df['Attribute'] == 'StudyDescription', 'Value'] = study_title
            
        result_df = define_df[['Attribute', 'Value']]
        
        print(f"Define表完成: {len(result_df)}行")
        return result_df

class DatasetsSheetProcessor(P21SheetProcessor):
    """Datasets工作表处理器"""
    
    def generate_datasets_data(self) -> pd.DataFrame:
        """生成Datasets表数据"""
        print("正在处理Datasets表单...")
        
        # 合并P21 datasets和项目spec datasets
        datasets_df = self.p21_data['datasets'].merge(
            self.spec_data['datasets'], 
            on='Dataset', 
            how='left',
            suffixes=('', '_spec')
        )
        
        # 更新字段 - 使用正确的列名
        if 'Description' in datasets_df.columns:
            datasets_df['Label'] = datasets_df['Description'].fillna(datasets_df['Label'])
        if 'Class_spec' in datasets_df.columns:
            datasets_df['Class'] = datasets_df['Class_spec'].fillna(datasets_df['Class'])
        elif 'Class' in datasets_df.columns and 'Derivation/Comment' in datasets_df.columns:
            datasets_df['Class'] = datasets_df['Derivation/Comment'].fillna(datasets_df['Class'])
        if 'Structure_spec' in datasets_df.columns:
            datasets_df['Structure'] = datasets_df['Structure_spec'].fillna(datasets_df['Structure'])
        if 'Keys' in datasets_df.columns:
            datasets_df['Key Variables'] = datasets_df['Keys'].fillna(datasets_df['Key Variables'])
        
        # 选择需要的列
        columns_to_keep = [
            'Dataset', 'Label', 'Class', 'SubClass', 'Structure', 'Key Variables', 
            'Standard', 'Has No Data', 'Repeating', 'Reference Data', 'Comment', 'Developer Notes'
        ]
        
        result_df = datasets_df[
            [col for col in columns_to_keep if col in datasets_df.columns]
        ]
        
        print(f"Datasets表完成: {len(result_df)}行")
        return result_df

class VariablesSheetProcessor(P21SheetProcessor):
    """Variables工作表处理器"""
    
    def generate_variables_data(self) -> pd.DataFrame:
        """生成Variables表数据"""
        print("正在处理Variables表单...")
        
        # 获取P21变量
        variables_df = self.p21_data['variables'].copy()
        
        # 分离SUPP和非SUPP变量
        supp_vars = variables_df[variables_df['Dataset'].str.contains('SUPP', na=False)].copy()
        non_supp_vars = variables_df[~variables_df['Dataset'].str.contains('SUPP', na=False)].copy()
        
        print(f"处理{len(non_supp_vars)}个非SUPP变量")
        print(f"保留{len(supp_vars)}个SUPP变量")
        
        # 处理非SUPP变量 - 与项目spec合并
        if not self.spec_data['dfspec'].empty:
            # 合并数据
            merged_vars = non_supp_vars.merge(
                self.spec_data['dfspec'],
                left_on=['Dataset', 'Variable'],
                right_on=['domain', 'Variable Name'],
                how='left',
                suffixes=('', '_spec')
            )
            
            print(f"合并后非SUPP变量数: {len(merged_vars)}")
            
            # 更新变量属性
            for idx, row in merged_vars.iterrows():
                # Codelist reference: prefer spec's codelist (DOMAIN.VARIABLE format)
                # which matches Codelists sheet ID; fall back to CDISC Submission Value
                spec_cl = row.get('Codelist_spec', '') if pd.notna(row.get('Codelist_spec')) else ''
                spec_cl = str(spec_cl).strip() if spec_cl else ''
                if spec_cl:
                    merged_vars.at[idx, 'Codelist'] = spec_cl
                elif pd.notna(row.get('CDISC Submission Value')):
                    cdisc_val = str(row['CDISC Submission Value']).replace('()', '').replace('(', '').replace(')', '')
                    if 'TESTN' not in cdisc_val and 'CATN' not in cdisc_val:
                        merged_vars.at[idx, 'Codelist'] = cdisc_val
                
                # 更新Origin和Pages
                if pd.notna(row.get('Origin_spec')):
                    merged_vars.at[idx, 'Origin'] = row['Origin_spec']
                if pd.notna(row.get('CRF Page')):
                    merged_vars.at[idx, 'Pages'] = row['CRF Page']
                
                # 更新Label
                if pd.notna(row.get('Variable Label')):
                    merged_vars.at[idx, 'Label'] = row['Variable Label']
                
                # 获取Method和Comment ID
                if pd.notna(row.get('Text')):
                    if self.methods_data:
                        method_match = [m for m in self.methods_data 
                                      if m.get('DERIVATION_COMMENT') == row['Text'] 
                                      and m.get('Sheet') == 'Variables sheet']
                        if method_match:
                            merged_vars.at[idx, 'Method'] = method_match[0]['id']
                    
                    if self.comments_data:
                        comment_match = [c for c in self.comments_data 
                                       if c.get('DERIVATION_COMMENT') == row['Text'] 
                                       and c.get('sheet') == 'Variables sheet']
                        if comment_match:
                            merged_vars.at[idx, 'Comment'] = comment_match[0]['id']
            
            non_supp_vars = merged_vars
        
        # 处理SUPP变量 - 如果有项目spec中的SUPP sheet，则与之对应
        if 'supp' in self.spec_data and not self.spec_data['supp'].empty:
            print(f"发现SUPP sheet，处理{len(supp_vars)}个SUPP变量")
            
            # 与SUPP sheet合并
            supp_merged = supp_vars.merge(
                self.spec_data['supp'],
                left_on='Variable',
                right_on='Variable Name',
                how='left',
                suffixes=('', '_supp')
            )
            
            # 更新SUPP变量属性
            for idx, row in supp_merged.iterrows():
                # 更新Label
                if pd.notna(row.get('Variable Label_supp')):
                    supp_merged.at[idx, 'Label'] = row['Variable Label_supp']
                
                # 更新Origin
                if pd.notna(row.get('Origin_supp')):
                    supp_merged.at[idx, 'Origin'] = row['Origin_supp']
                
                # 更新Codelist - 对于SUPP变量，RDOMAIN需要特殊处理
                if row['Variable'] == 'RDOMAIN':
                    # 从数据集名中提取domain
                    dataset_name = row['Dataset']
                    if dataset_name.startswith('SUPP'):
                        domain = dataset_name[4:]  # 去掉'SUPP'前缀
                        supp_merged.at[idx, 'Codelist'] = domain
                
                # 获取Method和Comment ID
                supp_text_col = 'Text_supp' if 'Text_supp' in row.index else 'Text'
                if pd.notna(row.get(supp_text_col)):
                    if self.methods_data:
                        method_match = [m for m in self.methods_data 
                                      if m.get('DERIVATION_COMMENT') == row[supp_text_col] 
                                      and m.get('Sheet') == 'SUPP sheet']
                        if method_match:
                            supp_merged.at[idx, 'Method'] = method_match[0]['id']
                    
                    if self.comments_data:
                        comment_match = [c for c in self.comments_data 
                                       if c.get('DERIVATION_COMMENT') == row[supp_text_col] 
                                       and c.get('sheet') == 'SUPP sheet']
                        if comment_match:
                            supp_merged.at[idx, 'Comment'] = comment_match[0]['id']
            
            supp_vars = supp_merged
        else:
            print("未发现SUPP sheet，保持原P21 SUPP变量格式")
        
        # 合并SUPP和非SUPP变量
        all_variables = pd.concat([non_supp_vars, supp_vars], ignore_index=True)
        
        # 选择需要的列
        columns_to_keep = [
            'Order', 'Dataset', 'Variable', 'Label', 'Data Type', 'Length', 
            'Significant Digits', 'Format', 'Mandatory', 'Assigned Value', 'Codelist', 
            'Common', 'Origin', 'Source', 'Pages', 'Method', 'Predecessor', 
            'Role', 'Has No Data', 'Comment', 'Developer Notes'
        ]
        
        # Overlay with external pages data when available
        if 'pages' in self.spec_data and not self.spec_data['pages'].empty:
            pages_df = self.spec_data['pages']
            pages_map = {}
            for _, pr in pages_df.iterrows():
                ds = str(pr.get('dataset', pr.get('Dataset', ''))).upper()
                var = str(pr.get('variable', pr.get('Variable', ''))).upper()
                pg = str(pr.get('pages', pr.get('Pages', '')))
                if ds and var and pg:
                    pages_map[(ds, var)] = pg
            if pages_map:
                updated = 0
                for idx, row in all_variables.iterrows():
                    key = (str(row.get('Dataset', '')).upper(), str(row.get('Variable', '')).upper())
                    if key in pages_map:
                        all_variables.at[idx, 'Pages'] = pages_map[key]
                        updated += 1
                print(f"  Pages overlay: updated {updated} variable page entries")

        result_df = all_variables[
            [col for col in columns_to_keep if col in all_variables.columns]
        ].sort_values(['Dataset', 'Order'])
        
        print(f"Variables表完成: {len(result_df)}行 (包含{len(supp_vars)}个SUPP变量)")
        return result_df

class ValueLevelSheetProcessor(P21SheetProcessor):
    """ValueLevel工作表处理器"""
    
    def generate_valuelevel_data(self, data_dir: Path) -> pd.DataFrame:
        """生成ValueLevel表数据"""
        print("正在处理Valuelevel表单...")
            
        if 'vlm' not in self.spec_data or self.spec_data['vlm'].empty:
            print("  VLM数据为空，跳过Valuelevel处理")
            return pd.DataFrame()
            
        vlm_df = self.spec_data['vlm'].copy()
        print(f"  处理{len(vlm_df)}条VLM记录")
        
        # 获取按 Where Clause 过滤后的变量长度
        xpt_cache = self._load_xpt_cache(data_dir)
        
        valuelevel_list = []
        
        for idx, row in vlm_df.iterrows():
            dataset = row.get('Dataset', '')
            variable = row.get('Variable', '')
            where_clause = row.get('Where Clause', '')
            
            # 按 Where Clause 过滤后计算长度
            max_len = self._get_filtered_length(
                xpt_cache, dataset, variable, where_clause)
            data_type = 'text'
            significant_digits = None
            
            # 处理Where Clause格式
            if where_clause:
                where_clause = str(where_clause).replace('"', '').replace('.', ' ')
            
            # 获取Codelist信息
            codelist = row.get('Controlled Terms or Format', '')
            if codelist == 'ISO8601' or codelist == 'durationDatetime':
                codelist = ''
            
            # 获取Method和Comment ID
            method_id = ''
            comment_id = ''
            
            derivation_comment = row.get('Derivation/Comment', '')
            if pd.notna(derivation_comment) and derivation_comment != '':
                # 查找对应的Method ID
                if self.methods_data:
                    method_match = [m for m in self.methods_data 
                                  if m.get('Sheet') == 'Valuelevel sheet' 
                                  and m.get('DERIVATION_COMMENT') == derivation_comment]
                    if method_match:
                        method_id = method_match[0]['id']
                
                # 查找对应的Comment ID
                if self.comments_data:
                    comment_match = [c for c in self.comments_data 
                                   if c.get('sheet') == 'Valuelevel sheet' 
                                   and c.get('DERIVATION_COMMENT') == derivation_comment]
                    if comment_match:
                        comment_id = comment_match[0]['id']
            
            # 构建Valuelevel记录 - 只包含P21标准字段
            valuelevel_record = {
                'Order': idx + 1,
                'Dataset': dataset,
                'Variable': variable,
                'Where Clause': where_clause,
                'Label': row.get('Label', ''),
                'Data Type': data_type,
                'Length': max_len,
                'Significant Digits': significant_digits,
                'Format': '',
                'Mandatory': 'No',
                'Assigned Value': '',
                'Codelist': codelist,
                'Origin': row.get('Origin', ''),
                'Source': row.get('Source', ''),
                'Pages': row.get('Pages', ''),
                'Method': method_id,
                'Predecessor': '',
                'Comment': comment_id,
                'Developer Notes': ''
            }
            
            valuelevel_list.append(valuelevel_record)
        
        result_df = pd.DataFrame(valuelevel_list)
        print(f"ValueLevel表完成: {len(result_df)}行")
        return result_df

    def _load_xpt_cache(self, data_dir: Path) -> dict:
        """Load XPT files into a cache keyed by uppercase dataset name."""
        cache = {}
        try:
            import xport
        except ImportError:
            print("    警告：未安装xport包，使用默认长度")
            return cache

        datasets = set()
        for _, row in self.spec_data['vlm'].iterrows():
            ds = str(row.get('Dataset', '')).upper().strip()
            if ds:
                datasets.add(ds)

        for ds in datasets:
            xpt_path = data_dir / f"{ds.lower()}.xpt"
            if not xpt_path.exists():
                continue
            try:
                with open(xpt_path, 'rb') as f:
                    df = xport.Dataset(f).to_dataframe()
                df.columns = df.columns.str.upper()
                cache[ds] = df
            except Exception as e:
                print(f"    警告：无法读取{xpt_path}: {e}")
        return cache

    @staticmethod
    def _get_filtered_length(xpt_cache: dict, dataset, variable, where_clause,
                             default: int = 200) -> int:
        """Compute max length of a variable after applying Where Clause filter."""
        import re
        ds = str(dataset).upper().strip()
        var = str(variable).upper().strip()
        if not ds or not var:
            return default

        df = xpt_cache.get(ds)
        if df is None or var not in df.columns:
            return default

        filtered = df
        wc = str(where_clause).strip() if where_clause else ''
        if wc:
            m = re.match(r'(\w+)\s+EQ\s+"([^"]*)"', wc)
            if not m:
                m = re.match(r'(\w+)\.EQ\."([^"]*)"', wc)
            if m:
                fc, fv = m.group(1).upper(), m.group(2)
                if fc in df.columns:
                    filtered = df[df[fc].astype(str).str.strip() == fv]

        col_data = filtered[var].dropna()
        if col_data.dtype == 'object':
            max_len = col_data.astype(str).str.len().max()
            return int(max_len) if not pd.isna(max_len) and max_len >= 1 else 1
        return 8

class CodelistsSheetProcessor(P21SheetProcessor):
    """Codelists工作表处理器"""
    
    # ACN术语映射字典 - 中文到英文CDISC Submission Value
    ACN_TERM_MAPPING = {
        '不适用': 'NOT APPLICABLE',  # 通常用于表示某个字段不适用于当前情况
        '药物未变（研究治疗药）': 'DOSE NOT CHANGED',  # 药物剂量保持不变
        '剂量中断(停止治疗药）': 'DRUG INTERRUPTED',  # 临时停止药物治疗
        '减量治疗': 'DOSE REDUCED',  # 降低药物剂量
        '永久停药': 'DRUG WITHDRAWN',  # 永久性停止药物治疗
    }
    
    def generate_codelists_data(self) -> pd.DataFrame:
        """生成Codelists表数据"""
        print("正在处理Codelists表单...")
        
        if not self.spec_data['codelists'].empty:
            # 使用正确的列名
            codelists_df = self.spec_data['codelists'].copy()
            
            # 检查列名并映射
            if 'CT/CodelistID' in codelists_df.columns:
                codelists_df['Id'] = codelists_df['CT/CodelistID']
            if 'CT/Codelist Name' in codelists_df.columns:
                codelists_df['Name'] = codelists_df['CT/Codelist Name']
            if 'Data Type' in codelists_df.columns:
                codelists_df['Data_Type'] = codelists_df['Data Type'].str.replace('Char', 'text')
            
            # Exclude MEDDRA / WHODRUG (these are dictionaries, not codelists)
            _dict_ids = {'MEDDRA', 'WHODRUG'}
            _id_col = 'Id' if 'Id' in codelists_df.columns else 'CT/CodelistID'
            if _id_col in codelists_df.columns:
                codelists_df = codelists_df[~codelists_df[_id_col].astype(str).str.strip().str.upper().isin(_dict_ids)]

            # Codelists - 没有version的记录（处理所有记录）
            spec_codelists = codelists_df[
                (codelists_df['Version'].isna()) | (codelists_df['Version'] == "")
            ].copy() if 'Version' in codelists_df.columns else codelists_df.copy()
            
            # 处理match_name逻辑（类似SAS）
            for idx, row in spec_codelists.iterrows():
                name = row.get('Name', '')
                if pd.notna(name):
                    name_lower = str(name).lower()
                    if 'subset' in name_lower:
                        match_name = str(name).split(',')[0]
                    elif 'yes only' in name_lower:
                        match_name = str(name).split('(')[0].strip()
                    else:
                        match_name = name
                    spec_codelists.at[idx, 'match_name'] = match_name
                else:
                    spec_codelists.at[idx, 'match_name'] = name
            
            codelists_list = []
            for idx, row in spec_codelists.iterrows():
                # 检查Term Decoded是否为空，如果为空则跳过这条记录
                term = row.get('Term Decoded', '')
                if pd.isna(term) or str(term).strip() == '' or str(term).strip().lower() == 'nan':
                    continue
                
                # 构建完整的P21标准Codelists记录
                existing_nci_cl = row.get('NCI Codelist Code', '')
                existing_nci_cl = str(existing_nci_cl).strip() if pd.notna(existing_nci_cl) else ''
                existing_nci_term = row.get('NCI Term Code', '')
                existing_nci_term = str(existing_nci_term).strip() if pd.notna(existing_nci_term) else ''
                codelist_record = {
                    'ID': row.get('Id', ''),
                    'Name': row.get('Name', ''),
                    'NCI Codelist Code': existing_nci_cl,
                    'Data Type': row.get('Data_Type', 'text'),
                    'Terminology': self.config.get('CT_SHEETNAME', ''),
                    'Comment': '',
                    'Order': str(row.get('cdnum', idx + 1)),
                    'Term': row.get('Term Decoded', ''),
                    'NCI Term Code': existing_nci_term,
                    'Decoded Value': row.get('Term  Dictionaries', ''),
                    'match_name': row.get('match_name', ''),
                    'term': row.get('Term Decoded', '')
                }
                
                # CT lookup only if NCI data not already populated (e.g. by fill-nci)
                if codelist_record['NCI Codelist Code'] == '' and \
                   'ct_code' in self.spec_data and not self.spec_data['ct_code'].empty:
                    ct_data = self.spec_data['ct_code']
                    
                    # 方法1：通过Name（CT Codelist Name）查找
                    codelist_name = row.get('Name', '')
                    codelist_id = row.get('Id', '')
                    # Try Name first, then fall back to Id for legacy submission-value format
                    lookup_val = ''
                    if pd.notna(codelist_name) and str(codelist_name).strip():
                        direct_match = ct_data[
                            (ct_data['Codelist Code'].isna() | (ct_data['Codelist Code'] == '')) &
                            (ct_data['Codelist Name'].str.lower() == str(codelist_name).strip().lower())
                        ]
                        if not direct_match.empty:
                            lookup_val = codelist_name
                    if lookup_val == '' and pd.notna(codelist_id) and str(codelist_id).strip():
                        direct_match = ct_data[
                            (ct_data['Codelist Code'].isna() | (ct_data['Codelist Code'] == '')) &
                            (ct_data['CDISC Submission Value'].str.lower() == str(codelist_id).strip().lower())
                        ]
                    
                    if lookup_val != '' or (pd.notna(codelist_id) and str(codelist_id).strip()):
                        if not direct_match.empty:
                            nci_codelist_code = direct_match.iloc[0].get('Code', '')
                            codelist_record['NCI Codelist Code'] = nci_codelist_code
                            
                            # 获取当前行的Term，然后在对应的NCI Codelist Code下查找匹配的term
                            term = row.get('Term Decoded', '')
                            if pd.notna(term) and term != '' and nci_codelist_code != '':
                                # 查找在NCI Codelist Code约束下，CDISC Submission Value匹配Term的记录
                                term_match = ct_data[
                                    (ct_data['Codelist Code'] == nci_codelist_code) &
                                    (ct_data['CDISC Submission Value'].str.strip() == str(term).strip())
                                ]
                                
                                if not term_match.empty:
                                    # 获取NCI Term Code
                                    nci_term_code = term_match.iloc[0].get('Code', '')
                                    codelist_record['NCI Term Code'] = nci_term_code
                                    
                                    # 只有在原始Decoded Value为空时才从CT中获取
                                    original_decoded_value = codelist_record.get('Decoded Value', '')
                                    if pd.isna(original_decoded_value) or str(original_decoded_value).strip() == '':
                                        # 获取CDISC Synonym作为Decoded Value
                                        cdisc_synonym = term_match.iloc[0].get('CDISC Synonym(s)', '')
                                        if pd.notna(cdisc_synonym) and cdisc_synonym != '':
                                            codelist_record['Decoded Value'] = cdisc_synonym
                                        else:
                                            # 如果没有synonym，使用CDISC Submission Value
                                            codelist_record['Decoded Value'] = term_match.iloc[0].get('CDISC Submission Value', '')
                                else:
                                    # 如果找不到匹配的term，设置NCI Term Code为[*]
                                    codelist_record['NCI Term Code'] = '[*]'
                                    # 只有在原始Decoded Value为空时才设置为[*]
                                    original_decoded_value = codelist_record.get('Decoded Value', '')
                                    if pd.isna(original_decoded_value) or str(original_decoded_value).strip() == '':
                                        codelist_record['Decoded Value'] = '[*]'
                            else:
                                # 如果没有Term或NCI Codelist Code，用[*]填补NCI Term Code
                                if codelist_record.get('NCI Term Code', '') == '':
                                    codelist_record['NCI Term Code'] = '[*]'
                                # 只有在原始Decoded Value为空时才用[*]填补
                                original_decoded_value = codelist_record.get('Decoded Value', '')
                                if (pd.isna(original_decoded_value) or str(original_decoded_value).strip() == '') and codelist_record.get('Decoded Value', '') == '':
                                    codelist_record['Decoded Value'] = '[*]'
                    
                    # 方法2：通过match_name（CT Codelist Name）查找
                    if codelist_record['NCI Codelist Code'] == '' or codelist_record['NCI Codelist Code'] == 'nan':
                        match_name = row.get('match_name', '')
                        if pd.notna(match_name) and match_name != '':
                            # 查找Codelist_Code为空且Codelist_Name匹配的记录
                            codelist_match = ct_data[
                                (ct_data['Codelist Code'].isna() | (ct_data['Codelist Code'] == '')) &
                                (ct_data['Codelist Name'].str.lower() == str(match_name).lower())
                            ]
                            
                            if not codelist_match.empty:
                                nci_codelist_code = codelist_match.iloc[0].get('Code', '')
                                codelist_record['NCI Codelist Code'] = nci_codelist_code
                                
                                # 第二步：通过term找到term code
                                term = row.get('Term Decoded', '')
                                if pd.notna(term) and term != '' and nci_codelist_code != '':
                                    # 查找Codelist_Code等于第一步找到的code且CDISC_Submission_Value匹配的记录
                                    term_match = ct_data[
                                        (ct_data['Codelist Code'] == nci_codelist_code) &
                                        (ct_data['CDISC Submission Value'].str.strip() == str(term).strip())
                                    ]
                                    
                                    if not term_match.empty:
                                        nci_term_code = term_match.iloc[0].get('Code', '')
                                        codelist_record['NCI Term Code'] = nci_term_code
                                        
                                        # 只有在原始Decoded Value为空时才从CT中获取
                                        original_decoded_value = codelist_record.get('Decoded Value', '')
                                        if pd.isna(original_decoded_value) or str(original_decoded_value).strip() == '':
                                            # 获取CDISC Synonym作为Decoded Value
                                            cdisc_synonym = term_match.iloc[0].get('CDISC Synonym(s)', '')
                                            if pd.notna(cdisc_synonym) and cdisc_synonym != '':
                                                codelist_record['Decoded Value'] = cdisc_synonym
                
                codelists_list.append(codelist_record)
            
            result_df = pd.DataFrame(codelists_list)
            
            # 移除临时字段
            if 'match_name' in result_df.columns:
                result_df = result_df.drop('match_name', axis=1)
            if 'term' in result_df.columns:
                result_df = result_df.drop('term', axis=1)
        else:
            result_df = pd.DataFrame()
        
        print(f"Codelists表完成: {len(result_df)}行")
        return result_df

class DictionariesSheetProcessor(P21SheetProcessor):
    """Dictionaries工作表处理器"""

    DICT_META = {
        'MEDDRA': {'Name': 'Adverse Event Dictionary', 'Dictionary': 'MEDDRA'},
        'WHODRUG': {'Name': 'Drug Dictionary', 'Dictionary': 'WHODRUG'},
    }

    def generate_dictionaries_data(self) -> pd.DataFrame:
        """生成Dictionaries表数据 — MEDDRA / WHODRUG 固定写入"""
        print("正在处理Dictionaries表单...")

        rows = []

        if not self.spec_data['codelists'].empty:
            codelists_df = self.spec_data['codelists'].copy()
            id_col = 'CT/CodelistID' if 'CT/CodelistID' in codelists_df.columns else 'Id'
            if id_col in codelists_df.columns:
                seen = set()
                for val in codelists_df[id_col].dropna().astype(str).str.strip().str.upper():
                    if val in self.DICT_META and val not in seen:
                        seen.add(val)
                        meta = self.DICT_META[val]
                        rows.append({
                            'Id': val,
                            'Name': meta['Name'],
                            'Data_Type': 'text',
                            'Dictionary': meta['Dictionary'],
                            'Version': '',
                        })

        if rows:
            result_df = pd.DataFrame(rows)
        else:
            result_df = pd.DataFrame()

        print(f"Dictionaries表完成: {len(result_df)}行")
        return result_df
        
class MethodsSheetProcessor(P21SheetProcessor):
    """Methods工作表处理器"""
    
    def generate_methods_data(self) -> pd.DataFrame:
        """生成Methods表数据"""
        print("正在处理Methods表单...")
        
        methods_list = []
        
        # 1. 来自variables的method
        if not self.spec_data['dfspec'].empty and 'Method' in self.spec_data['dfspec'].columns:
            variables_methods = self.spec_data['dfspec'][
                (self.spec_data['dfspec']['Method'].notna()) & 
                (self.spec_data['dfspec']['Method'] != "") &
                (self.spec_data['dfspec']['Method'] != "N")
            ].copy()
            
            for idx, row in variables_methods.iterrows():
                if pd.notna(row.get('Text')):
                    method_dict = self.parse_comment_method(row['Text'])
                    
                    meth_val = row.get('Method', '')
                    if meth_val not in ['Y', 'I']:
                        method_id = meth_val
                    else:
                        method_id = f"Method.{row['Variable Name']}"
                        
                    method_name = f"Algorithm to derive {method_id.replace('Method.', '')}"
                    method_type = "Imputation" if meth_val == 'I' else "Computation"
                    
                    methods_list.append({
                        'id': method_id,
                        'name': method_name,
                        'Description': method_dict['description'],
                        'Document': method_dict['document'],
                        'Pages': method_dict['pages'],
                        'Sheet': 'Variables sheet',
                        'Type': method_type,
                        'DERIVATION_COMMENT': row['Text']
                    })
        
        # 2. 来自VLM的method
        if 'vlm' in self.spec_data and 'Method' in self.spec_data['vlm'].columns:
            vlm_methods = self.spec_data['vlm'][
                (self.spec_data['vlm']['Method'].notna()) & 
                (self.spec_data['vlm']['Method'] != "") &
                (self.spec_data['vlm']['Method'] != "N")
            ].copy()
            
            for idx, row in vlm_methods.iterrows():
                if pd.notna(row.get('Derivation/Comment')):
                    method_dict = self.parse_comment_method(row['Derivation/Comment'])
                    
                    if row['Method'] not in ['Y', 'I']:
                        method_id = f"MT.{row['Method']}"
                    else:
                        method_id = f"MT.VLM.{idx+1}"
                        
                    method_name = f"Algorithm to derive {row['Variable']}"
                    method_type = "Imputation" if row['Method'] == 'I' else "Computation"
                    
                    methods_list.append({
                        'id': method_id,
                        'name': method_name,
                        'Description': method_dict['description'],
                        'Document': method_dict['document'],
                        'Pages': method_dict['pages'],
                        'Sheet': 'Valuelevel sheet', 
                        'Type': method_type,
                        'DERIVATION_COMMENT': row['Derivation/Comment']
                    })
        
        # Group by description text, find common suffix of variable names, update name
        if methods_list:
            desc_vars = {}
            for m in methods_list:
                desc = m['DERIVATION_COMMENT']
                var_name = m['name'].replace('Algorithm to derive ', '')
                desc_vars.setdefault(desc, []).append(var_name)
            desc_suffix = {desc: self._find_common_suffix(vars) for desc, vars in desc_vars.items()}
            for m in methods_list:
                m['name'] = f"Algorithm to derive {desc_suffix[m['DERIVATION_COMMENT']]}"

        result_df = pd.DataFrame(methods_list).drop_duplicates(
            subset=['name', 'id', 'Description']
        ) if methods_list else pd.DataFrame()
        
        # 保存到实例变量供其他处理器使用
        self.methods_data = methods_list
        
        print(f"Methods表完成: {len(result_df)}行")
        return result_df

    @staticmethod
    def _find_common_suffix(names: list) -> str:
        """从一组变量名中提取公共后缀，如 [AESEQ, DSSEQ, LBSEQ] → SEQ"""
        if not names:
            return ""
        if len(names) == 1:
            return names[0]
        first = names[0]
        suffix_len = len(first)
        for s in names[1:]:
            min_len = min(suffix_len, len(s))
            matched = 0
            for j in range(min_len):
                if first[-(j + 1)] == s[-(j + 1)]:
                    matched += 1
                else:
                    break
            suffix_len = matched
            if suffix_len == 0:
                break
        if suffix_len > 0:
            return first[-suffix_len:]
        return names[0]

class CommentsSheetProcessor(P21SheetProcessor):
    """Comments工作表处理器"""
    
    def generate_comments_data(self) -> pd.DataFrame:
        """生成Comments表数据"""
        print("正在处理Comments表单...")
        
        comments_list = []
        
        # 1. 来自datasets的comment
        comment_col = 'Derivation/Comment'
        
        if comment_col in self.spec_data['datasets'].columns:
            datasets_comments = self.spec_data['datasets'][
                (self.spec_data['datasets'][comment_col].notna()) & 
                (self.spec_data['datasets'][comment_col] != "")
            ].copy()
            
            for idx, row in datasets_comments.iterrows():
                comment_dict = self.parse_comment_method(row[comment_col])
                comments_list.append({
                    'id': f"Comment.D.{idx+1}",
                    'Description': comment_dict['description'],
                    'Document': comment_dict['document'],
                    'Pages': comment_dict['pages'],
                    'sheet': 'Datasets sheet',
                    'DERIVATION_COMMENT': row[comment_col]
                })
        
        # 2. 来自variables的comment  
        if not self.spec_data['dfspec'].empty and 'Comment' in self.spec_data['dfspec'].columns:
            variables_comments = self.spec_data['dfspec'][
                (self.spec_data['dfspec']['Comment'].notna()) & 
                (self.spec_data['dfspec']['Comment'] != "") &
                (self.spec_data['dfspec']['Comment'] != "N")
            ].copy()
            
            for idx, row in variables_comments.iterrows():
                if pd.notna(row.get('Text')):
                    comment_dict = self.parse_comment_method(row['Text'])
                    comment_id = row['Comment'] if row['Comment'] not in ['Y'] else f"Comment.{row['Variable Name']}"
                    
                    comments_list.append({
                        'id': comment_id,
                        'Description': comment_dict['description'],
                        'Document': comment_dict['document'], 
                        'Pages': comment_dict['pages'],
                        'sheet': 'Variables sheet',
                        'DERIVATION_COMMENT': row['Text']
                    })
        
        # 3. 来自VLM的comment
        if 'vlm' in self.spec_data and 'Comment' in self.spec_data['vlm'].columns:
            vlm_comments = self.spec_data['vlm'][
                (self.spec_data['vlm']['Comment'].notna()) & 
                (self.spec_data['vlm']['Comment'] != "") &
                (self.spec_data['vlm']['Comment'] != "N")
            ].copy()
            
            for idx, row in vlm_comments.iterrows():
                if pd.notna(row.get('Derivation/Comment')):
                    comment_dict = self.parse_comment_method(row['Derivation/Comment'])
                    comment_id = row['Comment'] if row['Comment'] not in ['Y'] else f"Comment.VLM.{idx+1}"
                    
                    comments_list.append({
                        'id': comment_id,
                        'Description': comment_dict['description'],
                        'Document': comment_dict['document'],
                        'Pages': comment_dict['pages'], 
                        'sheet': 'Valuelevel sheet',
                        'DERIVATION_COMMENT': row['Derivation/Comment']
                    })
        
        # 保存到实例变量供其他处理器使用
        self.comments_data = comments_list
        
        result_df = pd.DataFrame(comments_list)
        print(f"Comments表完成: {len(result_df)}行")
        return result_df

class DocumentsSheetProcessor(P21SheetProcessor):
    """Documents工作表处理器"""
    
    def generate_documents_data(self) -> pd.DataFrame:
        """生成Documents表数据"""
        print("正在处理Documents表单...")
        
        documents_list = [
            {'id': 'csdrg', 'title': "Reviewer's Guide", 'href': 'csdrg.pdf'},
            {'id': 'acrf', 'title': 'Annotated Case Report Form', 'href': 'acrf.pdf'}
        ]
        
        result_df = pd.DataFrame(documents_list)
        print(f"Documents表完成: {len(result_df)}行")
        return result_df

class ModularP21Processor:
    """模块化P21处理器 - 主控制器"""
    
    def __init__(self, config=None):
        """初始化处理器"""
        self.config = config if config is not None else CONFIG
        self.setup_paths()
        self.spec_data = {}
        self.p21_data = {}
        self.output_data = {}

        # 创建输出目录
        create_dir(Path(self.config['OUTPUT_DIR']))
        
    def setup_paths(self):
        """设置路径"""
        self.base_dir = Path(self.config['BASE_DIR'])

        # 检查是否有Web传入的文件路径
        if 'SPEC_FILE_PATH' in self.config and 'TEMPLATE_FILE_PATH' in self.config:
            # Web模式：使用上传的文件路径
            self.spec_file = Path(self.config['SPEC_FILE_PATH'])
            self.p21_file = Path(self.config['TEMPLATE_FILE_PATH'])
        else:
            # 本地模式：使用默认路径
            self.spec_dir = self.base_dir / "Python" / "define" / "项目Spec"
            self.p21_dir = self.base_dir / "Python" / "define" / "p21空spec"
            self.spec_file = self.spec_dir / self.config['SPEC']
            self.p21_file = self.p21_dir / self.config['P21_SPEC']

        self.data_dir = self.base_dir / "Python" / "define" / "sdtm define package"
        self.output_dir = self.base_dir / "Python" / "output" / f"sdtm_define_fixed_{datetime.now().strftime('%Y%m%d')}.xlsx"

    def load_data(self) -> None:
        """加载所有必要的数据文件"""
        print("正在加载数据文件...")
        
        cache_dir = self.base_dir / "Python" / "cache"
        cache_dir.mkdir(exist_ok=True)
        
        p21_cache = cache_dir / "p21_data.json"
        spec_cache = cache_dir / "spec_data.json"
        
        # 检查缓存文件是否存在且较新
        # 对于Web模式，不使用缓存，直接读取文件
        use_p21_cache = False
        use_spec_cache = False

        if hasattr(self, 'spec_dir') and hasattr(self, 'p21_dir'):
            # 本地模式才考虑缓存
            use_p21_cache = (p21_cache.exists() and
                            p21_cache.stat().st_mtime > self.p21_file.stat().st_mtime)
            use_spec_cache = (spec_cache.exists() and
                             spec_cache.stat().st_mtime > self.spec_file.stat().st_mtime)
        
        # 导入P21数据
        if use_p21_cache:
            print("  - 从缓存导入P21数据...")
            with open(p21_cache, 'r', encoding='utf-8') as f:
                cached_data = json.load(f)
            self.p21_data = {k: pd.DataFrame(v) for k, v in cached_data.items()}
        else:
            print("  - 导入P21 Excel文件...")
            self.p21_data = {
                'study': read_excel_sheet(self.p21_file, 'Define'),
                'datasets': read_excel_sheet(self.p21_file, 'Datasets'),
                'variables': read_excel_sheet(self.p21_file, 'Variables')
            }
            # 保存缓存
            print("  - 保存P21缓存...")
            cached_data = {k: v.to_dict('records') for k, v in self.p21_data.items()}
            with open(p21_cache, 'w', encoding='utf-8') as f:
                json.dump(cached_data, f, ensure_ascii=False, indent=2)
        
        # 导入项目spec数据
        if use_spec_cache:
            print("  - 从缓存导入Spec数据...")
            with open(spec_cache, 'r', encoding='utf-8') as f:
                cached_data = json.load(f)
            self.spec_data = {k: pd.DataFrame(v) for k, v in cached_data.items()}
        else:
            print("  - 导入项目Spec Excel文件...")
            self.spec_data = {
                'datasets': read_excel_sheet(self.spec_file, 'TOC'),
                'codelists': read_excel_sheet(self.spec_file, 'codelists'),
                'vlm': read_excel_sheet(self.spec_file, 'VLM'),
                'pages': pd.DataFrame(),
            }

            # 尝试导入SUPP sheet（如果存在）
            try:
                supp_df = read_excel_sheet(self.spec_file, 'SUPP')
                if not supp_df.empty:
                    self.spec_data['supp'] = supp_df
                    print("  - 成功导入SUPP sheet")
                else:
                    self.spec_data['supp'] = pd.DataFrame()
            except Exception as e:
                print(f"  - 未发现SUPP sheet或导入失败: {e}")
                self.spec_data['supp'] = pd.DataFrame()
            
            # 尝试导入SDTM Terminology表（如果存在）
            try:
                ct_file = self.base_dir / "Python" / "define" / "CT" / "SDTM Terminology_2023-06-30.xls"
                print(f"  - 检查CT文件路径: {ct_file}")
                print(f"  - CT文件是否存在: {ct_file.exists()}")
                if ct_file.exists():
                    ct_df = read_excel_sheet(ct_file, 'SDTM Terminology 2023-06-30')
                    print(f"  - CT数据读取结果: {len(ct_df)}行, {len(ct_df.columns) if not ct_df.empty else 0}列")
                    if not ct_df.empty:
                        print(f"  - CT列名: {list(ct_df.columns)}")
                        self.spec_data['ct_code'] = ct_df
                        print("  - 成功导入SDTM Terminology表")
                    else:
                        print("  - CT数据为空")
                        self.spec_data['ct_code'] = pd.DataFrame()
                else:
                    print("  - 未发现SDTM Terminology文件")
                    self.spec_data['ct_code'] = pd.DataFrame()
            except Exception as e:
                print(f"  - 导入SDTM Terminology失败: {e}")
                import traceback
                traceback.print_exc()
                self.spec_data['ct_code'] = pd.DataFrame()
            
            # 导入domain specs
            self.import_domain_specs()
            
            # 保存完整缓存
            print("  - 保存Spec缓存...")
            cached_data = {k: v.to_dict('records') for k, v in self.spec_data.items()}
            with open(spec_cache, 'w', encoding='utf-8') as f:
                json.dump(cached_data, f, ensure_ascii=False, indent=2)
        
        # 如果使用了spec缓存，仍需要导入domain specs和SUPP sheet
        if use_spec_cache:
            if 'dfspec' not in self.spec_data:
                print("  - 缓存中缺少domain数据，重新导入...")
                self.import_domain_specs()
            
            if 'supp' not in self.spec_data:
                print("  - 缓存中缺少SUPP数据，尝试导入...")
                try:
                    supp_df = read_excel_sheet(self.spec_file, 'SUPP')
                    if not supp_df.empty:
                        self.spec_data['supp'] = supp_df
                        print("  - 成功导入SUPP sheet")
                    else:
                        self.spec_data['supp'] = pd.DataFrame()
                except Exception as e:
                    print(f"  - 未发现SUPP sheet或导入失败: {e}")
                    self.spec_data['supp'] = pd.DataFrame()
        
        # Override with external data if provided (from web backend via JSON)
        if hasattr(self, '_external_vlm') and self._external_vlm is not None and not self._external_vlm.empty:
            print(f"  - 使用外部VLM数据覆盖: {len(self._external_vlm)} 条记录")
            self.spec_data['vlm'] = self._external_vlm

        if hasattr(self, '_external_codelist') and self._external_codelist is not None and not self._external_codelist.empty:
            print(f"  - 使用外部Codelist数据覆盖: {len(self._external_codelist)} 条记录")
            cl_df = self._external_codelist.copy()
            cl_col_map = {
                'vcd': 'CT/CodelistID',
                'vlabel': 'CT/Codelist Name',
                'code': 'Term Decoded',
                'codeDes': 'Term  Dictionaries',
                'type': 'Data Type',
                'cdnum': 'cdnum',
                'codeVer': 'Version',
                'nciCodelistCode': 'NCI Codelist Code',
                'nciTermCode': 'NCI Term Code',
            }
            for old_col, new_col in cl_col_map.items():
                if old_col in cl_df.columns and new_col not in cl_df.columns:
                    cl_df.rename(columns={old_col: new_col}, inplace=True)
            self.spec_data['codelists'] = cl_df

        if hasattr(self, '_external_pages') and self._external_pages is not None and not self._external_pages.empty:
            print(f"  - 使用外部Pages数据覆盖: {len(self._external_pages)} 条记录")
            self.spec_data['pages'] = self._external_pages

        if hasattr(self, '_external_spec') and self._external_spec is not None and not self._external_spec.empty:
            print(f"  - 使用外部Spec(dfspec)数据覆盖: {len(self._external_spec)} 条记录")
            self.spec_data['dfspec'] = self._external_spec

        if 'dfspec' not in self.spec_data or self.spec_data['dfspec'].empty:
            print("  - dfspec为空，尝试从文件导入domain规范...")
            self.import_domain_specs()

        print(f"数据加载完成！")
    
    def import_domain_specs(self):
        """导入各个domain的spec表单"""
        print("  - 导入domain规范表单...")
        
        # 只导入主要的domain，避免处理时间过长
        main_domains = ['DM', 'AE', 'VS', 'LB', 'EX', 'CM', 'MH', 'DS', 'IE', 'SE', 'PC', 'PP', 'PE', 'QS']
        
        self.spec_data['dfspec'] = pd.DataFrame()
        imported_count = 0
        
        for domain in main_domains:
            try:
                print(f"    导入domain: {domain}")
                domain_df = read_excel_sheet(self.spec_file, domain)
                if not domain_df.empty:
                    domain_df['domain'] = domain
                    self.spec_data['dfspec'] = pd.concat([self.spec_data['dfspec'], domain_df], ignore_index=True)
                    imported_count += 1
            except Exception as e:
                print(f"    跳过{domain}: {e}")
                
        print(f"成功导入{imported_count}个domain表单")
    
    def generate_all_data(self) -> Dict[str, pd.DataFrame]:
        """生成所有表的数据"""
        results = {}
        
        print("开始生成所有表数据...")
        
        # 1. 生成Define表
        print("  1. 生成Define表...")
        define_processor = DefineSheetProcessor(self.config, self.spec_data, self.p21_data)
        results['Define'] = define_processor.generate_define_data()
        
        # 2. 生成Datasets表
        print("  2. 生成Datasets表...")
        datasets_processor = DatasetsSheetProcessor(self.config, self.spec_data, self.p21_data)
        results['Datasets'] = datasets_processor.generate_datasets_data()
        
        # 3. 生成Comments表（需要先处理，供其他表引用）
        print("  3. 生成Comments表...")
        comments_processor = CommentsSheetProcessor(self.config, self.spec_data, self.p21_data)
        results['Comments'] = comments_processor.generate_comments_data()
        
        # 4. 生成Methods表（需要先处理，供其他表引用）
        print("  4. 生成Methods表...")
        methods_processor = MethodsSheetProcessor(self.config, self.spec_data, self.p21_data)
        results['Methods'] = methods_processor.generate_methods_data()
        
        # 5. 生成Variables表（需要Methods和Comments数据）
        print("  5. 生成Variables表...")
        variables_processor = VariablesSheetProcessor(self.config, self.spec_data, self.p21_data)
        variables_processor.methods_data = methods_processor.methods_data
        variables_processor.comments_data = comments_processor.comments_data
        results['Variables'] = variables_processor.generate_variables_data()
        
        # 6. 生成ValueLevel表（需要Methods和Comments数据）
        print("  6. 生成ValueLevel表...")
        valuelevel_processor = ValueLevelSheetProcessor(self.config, self.spec_data, self.p21_data)
        valuelevel_processor.methods_data = methods_processor.methods_data
        valuelevel_processor.comments_data = comments_processor.comments_data
        results['ValueLevel'] = valuelevel_processor.generate_valuelevel_data(self.data_dir)
        
        # 7. 生成Codelists表
        print("  7. 生成Codelists表...")
        codelists_processor = CodelistsSheetProcessor(self.config, self.spec_data, self.p21_data)
        results['Codelists'] = codelists_processor.generate_codelists_data()
        
        # 8. 生成Dictionaries表
        print("  8. 生成Dictionaries表...")
        dictionaries_processor = DictionariesSheetProcessor(self.config, self.spec_data, self.p21_data)
        results['Dictionaries'] = dictionaries_processor.generate_dictionaries_data()
        
        # 9. 生成Documents表
        print("  9. 生成Documents表...")
        documents_processor = DocumentsSheetProcessor(self.config, self.spec_data, self.p21_data)
        results['Documents'] = documents_processor.generate_documents_data()
        
        print("所有表数据生成完成！")
        return results
    
    def export_all_results(self, all_data: Dict[str, pd.DataFrame]) -> None:
        """导出所有结果到Excel文件"""
        print(f"正在导出完整结果到: {self.output_dir}")
        
        try:
            # 复制原始模板文件
            template_file = self.p21_dir / self.config['P21_SPEC']
            shutil.copy2(template_file, self.output_dir)
            
            # 按P21输出顺序更新各个表单
            p21_sheet_order = ['Define', 'Datasets', 'Variables', 'ValueLevel', 'Codelists', 'Dictionaries', 'Methods', 'Comments', 'Documents']
            
            for sheet_name in p21_sheet_order:
                if sheet_name in all_data and not all_data[sheet_name].empty:
                    print(f"  导出{sheet_name}表: {len(all_data[sheet_name])}行")
                    export_to_excel(all_data[sheet_name], self.output_dir, sheet_name)
                else:
                    print(f"  {sheet_name}表为空，保持原格式")
            
            print("导出完成！")
            print(f"  文件位置: {self.output_dir}")
            print(f"  文件大小: {self.output_dir.stat().st_size / 1024 / 1024:.1f}MB")
            
            # 显示详细统计
            print("\n完整统计:")
            for sheet_name, df in all_data.items():
                non_empty_count = 0
                if not df.empty:
                    for col in df.columns:
                        if col in df.columns:
                            non_empty_count += df[col].notna().sum()
                
                print(f"  {sheet_name}: {len(df)}行, {non_empty_count}个非空值")
            
        except Exception as e:
            print(f"导出失败: {e}")
            import traceback
            traceback.print_exc()
    
    def run(self) -> None:
        """运行完整流程"""
        print("=" * 60)
        print("模块化P21 Define.xml生成器")
        print("每个sheet独立工作，类似SAS程序结构")
        print("=" * 60)
        
        start_time = time.time()
        
        try:
            # 1. 加载数据
            self.load_data()
            
            # 2. 生成所有表数据
            all_data = self.generate_all_data()
            
            # 3. 导出结果
            self.export_all_results(all_data)
            
            total_time = time.time() - start_time
            print(f"\n完整任务完成！总用时: {total_time:.1f}秒")
            
        except Exception as e:
            print(f"任务失败: {e}")
            import traceback
            traceback.print_exc()

def main():
    """主函数"""
    # 读取配置
    config, task_id = read_config_from_args()

    print("=" * 60)
    print("模块化P21 Define.xml生成器")
    if task_id:
        print(f"任务ID: {task_id}")
    print("=" * 60)

    try:
        # 使用配置创建处理器
        processor = ModularP21Processor(config)
        processor.run()

        print("\n任务完成成功！")
        sys.exit(0)  # 成功退出

    except Exception as e:
        print(f"\n任务执行失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)  # 失败退出

if __name__ == '__main__':
    main()