import pandas as pd
import os
import re

# 文件路径
spec_file = r"E:\JAVAPROJ\008_defineXML\Sas\define\项目Spec\spec.xlsx"
vlm_file = r"E:\JAVAPROJ\008_defineXML\Sas\define\项目Spec\out\vlm_codelists.xlsx"
mapping_file = r"E:\JAVAPROJ\008_defineXML\Sas\define\项目Spec\variable_page_mapping.xlsx"
annots_file = r"E:\JAVAPROJ\008_defineXML\Sas\define\项目Spec\Annots2.xlsx"

print("读取所需文件...")
try:
    mapping_df = pd.read_excel(mapping_file)
    annots_df = pd.read_excel(annots_file)
    print(f"映射文件已读取，共{len(mapping_df)}行")
    print(f"标注文件已读取，共{len(annots_df)}行")
except Exception as e:
    print(f"读取文件失败: {e}")
    exit()

# 检查spec文件
print(f"\n检查spec文件...")
try:
    excel_file = pd.ExcelFile(spec_file, engine='openpyxl')
    print(f"成功打开spec文件")
except Exception as e:
    print(f"无法打开spec文件: {e}")
    exit()

def find_domain_variable_in_annotations(domain, variable_name, supp_flag, annots_df):
    """在Annots2中查找domain变量的页码"""
    if annots_df is None or annots_df.empty:
        return []
    
    matches = []
    
    for idx, row in annots_df.iterrows():
        content = str(row.get('Contents', '')).strip()
        if not content:
            continue
        
        # 根据SUPP标志使用不同的匹配策略
        patterns = []
        
        if str(supp_flag).upper() == 'Y':
            # SUPP=Y时，搜索QNAM=变量名的格式
            patterns = [
                f"QNAM\\s*=\\s*{re.escape(variable_name)}(?=\\s|$)",  # QNAM=TESTNUMC
                f"QNAM\\s*=\\s*[\"']{re.escape(variable_name)}[\"'](?=\\s|$)",  # QNAM="TESTNUMC"
                f"(?<![A-Za-z0-9]){re.escape(variable_name)}(?![A-Za-z0-9]).*QNAM",  # TESTNUMC...QNAM
                f"QNAM.*(?<![A-Za-z0-9]){re.escape(variable_name)}(?![A-Za-z0-9])",  # QNAM...TESTNUMC
            ]
        else:
            # SUPP!=Y时，直接搜索变量名
            patterns = [
                f"(?<![A-Za-z0-9]){re.escape(variable_name)}(?![A-Za-z0-9])",  # 完整单词匹配
                f"{re.escape(variable_name)}\\s*=",  # 变量名=
                f"when\\s+{re.escape(variable_name)}\\s*=",  # when 变量名=
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
                break  # 找到一个匹配就停止
    
    return matches

def get_pages_for_variable(domain, variable, mapping_df):
    """获取指定domain和变量的所有页码"""
    matches = mapping_df[
        (mapping_df['Domain'] == domain) & 
        (mapping_df['Variable'] == variable)
    ]
    
    if matches.empty:
        return ""
    
    pages = sorted(list(set(matches['Found_Page'].tolist())))
    return " ".join(str(p) for p in pages)

def get_pages_for_vlm_variable(dataset, variable, where_clause, mapping_df):
    """获取VLM变量的页码"""
    matches = mapping_df[
        (mapping_df['Type'] == 'VLM_WhereClause') &
        (mapping_df['Dataset'] == dataset) & 
        (mapping_df['Variable'] == variable)
    ]
    
    if not where_clause:
        if matches.empty:
            return ""
    else:
        matches = matches[matches['Where_Clause'] == where_clause]
        if matches.empty:
            return ""
    
    pages = sorted(list(set(matches['Found_Page'].tolist())))
    return " ".join(str(p) for p in pages)

def check_variable_in_annots(variable_name, annots_df):
    """检查变量名是否在Annots2中出现"""
    for idx, row in annots_df.iterrows():
        content = str(row.get('Contents', '')).strip()
        if variable_name in content:
            return True
    return False

# QC检查结果
qc_issues = []

# 更新spec.xlsx中的domain表并进行QC检查
print("\n开始更新spec.xlsx中的domain表并进行QC检查...")

domains_to_update = ['DM', 'AE', 'PC', 'PP', 'PE', 'LB', 'EG', 'VS', 'EX', 'CM', 'MH']  # 添加更多域
updated_sheets = {}
total_updates = 0

for domain in domains_to_update:
    if domain in excel_file.sheet_names:
        print(f"\n处理domain: {domain}")
        try:
            df = pd.read_excel(spec_file, sheet_name=domain, engine='openpyxl')
            
            if 'Variable Name' in df.columns and 'Origin' in df.columns and 'CRF Page' in df.columns:
                updates_count = 0
                
                for idx, row in df.iterrows():
                    origin = str(row.get('Origin', '')).strip()
                    variable_name = row.get('Variable Name', '')
                    supp_flag = row.get('SUPP', '')  # 获取SUPP标志
                    
                    if origin.upper() == 'CRF' and variable_name:
                        # 处理SUPP标志，包括NaN情况
                        if pd.isna(supp_flag) or supp_flag == '' or str(supp_flag).upper() == 'NAN':
                            supp_processed = 'N'
                        else:
                            supp_processed = str(supp_flag).upper()
                        
                        matches = find_domain_variable_in_annotations(domain, variable_name, supp_processed, annots_df)
                        if matches:
                            pages = []
                            for match in matches:
                                pages.append(match['page'])
                            pages = sorted(list(set(pages)))
                            pages_str = " ".join(str(p) for p in pages)
                            
                            # 确保列的数据类型为字符串
                            df['CRF Page'] = df['CRF Page'].astype(str)
                            df.at[idx, 'CRF Page'] = pages_str
                            updates_count += 1
                            print(f"  更新 {variable_name} (SUPP={supp_processed}): {pages_str}")
                        else:
                            # QC问题：Origin=CRF但在Annots2中未找到
                            qc_issues.append({
                                'Issue_Type': 'Missing_in_Annots',
                                'Domain': domain,
                                'Variable': variable_name,
                                'Origin': origin,
                                'SUPP': supp_processed,
                                'Description': f'{domain}.{variable_name} (SUPP={supp_processed}) 标记为Origin=CRF但在CRF标注中未找到',
                                'Current_CRF_Page': row.get('CRF Page', ''),
                                'Recommended_Action': '检查CRF是否确实包含此变量，或修改Origin'
                            })
                            # 设置为空而不是nan
                            df['CRF Page'] = df['CRF Page'].astype(str)
                            df.at[idx, 'CRF Page'] = ''
                            print(f"  QC问题: {variable_name} (SUPP={supp_processed}) 在CRF中未找到")
                    elif origin.upper() != 'CRF':
                        # 非CRF来源的变量，CRF Page应该为空
                        df['CRF Page'] = df['CRF Page'].astype(str)
                        df.at[idx, 'CRF Page'] = ''
                
                updated_sheets[domain] = df
                total_updates += updates_count
                print(f"  {domain}域共更新{updates_count}个变量的页码")
            else:
                print(f"  {domain}域缺少必要的列")
                updated_sheets[domain] = df
        except Exception as e:
            print(f"  读取{domain}域失败: {e}")
    else:
        print(f"  {domain}域不存在")

# 更新VLM文件并进行QC检查
print(f"\n开始更新VLM文件并进行QC检查...")
try:
    vlm_df = pd.read_excel(vlm_file)
    print(f"VLM文件已读取，共{len(vlm_df)}行")
    
    if 'Origin' in vlm_df.columns and 'Pages' in vlm_df.columns:
        vlm_updates = 0
        
        for idx, row in vlm_df.iterrows():
            origin = str(row.get('Origin', '')).strip()
            dataset = row.get('Dataset', '')
            variable = row.get('Variable', '')
            where_clause = row.get('Where Clause', '')
            
            if origin.upper() == 'CRF':
                pages = get_pages_for_vlm_variable(dataset, variable, where_clause, mapping_df)
                if pages:
                    vlm_df['Pages'] = vlm_df['Pages'].astype(str)
                    vlm_df.at[idx, 'Pages'] = str(pages)
                    vlm_updates += 1
                    print(f"  更新 {dataset}.{variable}: {pages}")
                else:
                    # QC问题：VLM变量在CRF中未找到
                    qc_issues.append({
                        'Issue_Type': 'Missing_VLM_in_Annots',
                        'Domain': dataset,
                        'Variable': variable,
                        'Origin': origin,
                        'Where_Clause': where_clause,
                        'Description': f'{dataset}.{variable} (Where: {where_clause}) 标记为Origin=CRF但在CRF标注中未找到',
                        'Current_CRF_Page': row.get('Pages', ''),
                        'Recommended_Action': '检查Where条件是否正确，或检查CRF是否包含此条件'
                    })
                    vlm_df['Pages'] = vlm_df['Pages'].astype(str)
                    vlm_df.at[idx, 'Pages'] = ''
                    print(f"  QC问题: {dataset}.{variable} 条件在CRF中未找到")
            else:
                # 非CRF来源的变量，Pages应该为空
                vlm_df['Pages'] = vlm_df['Pages'].astype(str)
                vlm_df.at[idx, 'Pages'] = ''
        
        print(f"VLM文件共更新{vlm_updates}个变量的页码")
        total_updates += vlm_updates
    else:
        print("VLM文件缺少必要的列")
        vlm_df = None
        
except Exception as e:
    print(f"读取VLM文件失败: {e}")
    vlm_df = None

# 检查Annots2中有但Spec中未用到的变量
print(f"\n检查Annots2中有但Spec中可能遗漏的变量...")

# 从Annots2中提取所有可能的变量名
annots_variables = set()

for idx, row in annots_df.iterrows():
    content = str(row.get('Contents', '')).strip()
    # 提取可能的变量名模式
    # 模式1: VARIABLE = 或 VARIABLE=
    matches1 = re.findall(r'([A-Z][A-Z0-9]{2,})\s*=', content)
    # 模式2: when VARIABLE=
    matches2 = re.findall(r'when\s+([A-Z][A-Z0-9]{2,})\s*=', content)
    # 模式3: QNAM=VARIABLE
    matches3 = re.findall(r'QNAM\s*=\s*([A-Z][A-Z0-9]{2,})', content)
    
    annots_variables.update(matches1)
    annots_variables.update(matches2)
    annots_variables.update(matches3)

# 从Spec中获取所有CRF变量
spec_crf_variables = set()
for domain in updated_sheets:
    df = updated_sheets[domain]
    if 'Variable Name' in df.columns and 'Origin' in df.columns:
        crf_vars = df[df['Origin'].str.upper() == 'CRF']['Variable Name'].tolist()
        spec_crf_variables.update(crf_vars)

# 找出在Annots2中但不在Spec中的变量
missing_in_spec = annots_variables - spec_crf_variables

if missing_in_spec:
    print(f"发现{len(missing_in_spec)}个可能遗漏的变量:")
    for var in sorted(missing_in_spec):
        # 检查这个变量在哪些页面出现
        pages = []
        for idx, row in annots_df.iterrows():
            content = str(row.get('Contents', '')).strip()
            if var in content:
                pages.append(row.get('page'))
        
        unique_pages = sorted(list(set(pages)))
        qc_issues.append({
            'Issue_Type': 'Potential_Missing_Variable',
            'Domain': 'Unknown',
            'Variable': var,
            'Origin': 'Potential_CRF',
            'Description': f'变量 {var} 在CRF标注中发现但可能在Spec中遗漏',
            'Found_Pages': ' '.join(str(p) for p in unique_pages),
            'Recommended_Action': '检查是否需要在相应的Domain中添加此变量'
        })
        print(f"  {var}: 页 {' '.join(str(p) for p in unique_pages)}")

# 保存更新后的文件
print(f"\n开始保存更新后的文件...")

# 保存spec.xlsx
if updated_sheets:
    # 读取所有原始工作表
    all_sheets = {}
    try:
        for sheet_name in excel_file.sheet_names:
            if sheet_name in updated_sheets:
                all_sheets[sheet_name] = updated_sheets[sheet_name]
            else:
                all_sheets[sheet_name] = pd.read_excel(spec_file, sheet_name=sheet_name, engine='openpyxl')
        
        # 备份原文件
        backup_file = spec_file.replace('.xlsx', '_backup.xlsx')
        import shutil
        if not os.path.exists(backup_file):  # 避免重复备份
            shutil.copy2(spec_file, backup_file)
            print(f"原spec.xlsx已备份为: {backup_file}")
        
        # 保存更新后的文件
        with pd.ExcelWriter(spec_file, engine='openpyxl') as writer:
            for sheet_name, df in all_sheets.items():
                df.to_excel(writer, sheet_name=sheet_name, index=False)
        from excel_style import style_excel_file
        style_excel_file(spec_file)
        print(f"spec.xlsx已更新并保存")
    except Exception as e:
        print(f"保存spec.xlsx失败: {e}")

# 保存VLM文件
if vlm_df is not None:
    backup_vlm = vlm_file.replace('.xlsx', '_backup.xlsx')
    import shutil
    if not os.path.exists(backup_vlm):  # 避免重复备份
        shutil.copy2(vlm_file, backup_vlm)
        print(f"原VLM文件已备份为: {backup_vlm}")
    
    vlm_df.to_excel(vlm_file, index=False)
    from excel_style import style_excel_file
    style_excel_file(vlm_file)
    print(f"VLM文件已更新并保存")

# 生成QC报告
if qc_issues:
    qc_df = pd.DataFrame(qc_issues)
    qc_file = r"E:\JAVAPROJ\008_defineXML\Sas\define\项目Spec\QC_Report.xlsx"
    qc_df.to_excel(qc_file, index=False)
    from excel_style import style_excel_file
    style_excel_file(qc_file)
    print(f"\nQC报告已生成: {qc_file}")
    print(f"共发现{len(qc_issues)}个QC问题")
    
    # 按问题类型汇总
    issue_summary = qc_df['Issue_Type'].value_counts()
    print("\nQC问题汇总:")
    for issue_type, count in issue_summary.items():
        print(f"  {issue_type}: {count}个")
else:
    print("\n未发现QC问题")

print(f"\n更新完成！总共更新了{total_updates}个变量的页码信息")