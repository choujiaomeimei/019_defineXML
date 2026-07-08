import pandas as pd
import re
import os

# 文件路径
annots_file = r"E:\JAVAPROJ\008_defineXML\Python\output\Spec_out\Annots2.xlsx"  # 提取的注释文件
spec_file = r"E:\JAVAPROJ\008_defineXML\Python\define\项目Spec\spec.xlsx"  # 可能包含domains和VLM信息
vlm_file = r"E:\JAVAPROJ\008_defineXML\Python\output\Spec_out\vlm_codelists.xlsx"  # VLM输出文件

# 读取Annots2.xlsx
print("读取Annots2.xlsx...")
try:
    annots_df = pd.read_excel(annots_file)
    print(f"Annots2.xlsx已读取，共{len(annots_df)}行")
    print(f"列名: {list(annots_df.columns)}")
    print(f"前5行:")
    print(annots_df.head())
except Exception as e:
    print(f"读取Annots2.xlsx失败: {e}")
    annots_df = None

# 读取spec.xlsx (可能包含多个工作表)
print("\n读取spec.xlsx...")
try:
    # 先查看有哪些工作表
    excel_file = pd.ExcelFile(spec_file)
    print(f"Spec.xlsx中的工作表: {excel_file.sheet_names}")
    
    # 尝试读取可能的domains表
    domains_df = None
    for sheet_name in excel_file.sheet_names:
        if 'domain' in sheet_name.lower() or 'variable' in sheet_name.lower() or sheet_name in ['AE', 'LB', 'DM', 'VS', 'PE', 'EG', 'PC', 'PP']:
            print(f"尝试读取工作表: {sheet_name}")
            df = pd.read_excel(spec_file, sheet_name=sheet_name)
            print(f"  列名: {list(df.columns)}")
            # 如果包含Variable Name列，存储为domains信息
            if 'Variable Name' in df.columns:
                if domains_df is None:
                    domains_df = df.copy()
                    domains_df['Domain'] = sheet_name  # 添加domain标识
                else:
                    df_temp = df.copy()
                    df_temp['Domain'] = sheet_name
                    domains_df = pd.concat([domains_df, df_temp], ignore_index=True)
                print(f"  找到Variable Name相关的工作表: {sheet_name}")
    
    if domains_df is not None:
        print(f"合并后的domains表共{len(domains_df)}行")
        print(f"前5个Variable Name:")
        print(domains_df[['Domain', 'Variable Name', 'SUPP']].head())
except Exception as e:
    print(f"读取spec.xlsx失败: {e}")
    domains_df = None

# 读取VLM相关文件
print("\n读取VLM相关文件...")
try:
    vlm_df = pd.read_excel(vlm_file)
    print(f"VLM文件已读取，共{len(vlm_df)}行")
    print(f"列名: {list(vlm_df.columns)}")
    if 'Where Clause' in vlm_df.columns:
        print(f"Where Clause列的前5个值:")
        print(vlm_df['Where Clause'].head())
    elif 'where' in ' '.join(vlm_df.columns).lower():
        where_col = [col for col in vlm_df.columns if 'where' in col.lower()][0]
        print(f"找到Where相关列: {where_col}")
        print(f"前5个值:")
        print(vlm_df[where_col].head())
except Exception as e:
    print(f"读取VLM文件失败: {e}")
    vlm_df = None

# 解析Where Clause并在Annots2中查找匹配的页码
def parse_where_clause(where_clause):
    """解析Where Clause，返回变量名和值"""
    if pd.isna(where_clause) or not where_clause:
        return None, None
    
    # 匹配格式: VARIABLE EQ "VALUE" 或 VARIABLE = "VALUE"
    import re
    match = re.search(r'(\w+)\s*(?:EQ|=)\s*["\']?([^"\']+)["\']?', str(where_clause), re.IGNORECASE)
    if match:
        variable = match.group(1).strip()
        value = match.group(2).strip()
        return variable, value
    return None, None

def find_variable_in_annotations(variable, value, annots_df):
    """在Annots2中查找匹配的变量值和页码"""
    if annots_df is None or annots_df.empty:
        return []
    
    matches = []
    # 在Contents列中搜索匹配项
    for idx, row in annots_df.iterrows():
        content = str(row.get('Contents', '')).strip()
        if not content:
            continue
            
        # 多种匹配模式
        patterns = [
            f"{variable}\\s*=\\s*{re.escape(value)}(?=\\s|$)",  # LBTESTCD = PE (后跟空格或结束)
            f"{variable}\\s*EQ\\s*{re.escape(value)}(?=\\s|$)",  # LBTESTCD EQ PE (后跟空格或结束)
            f"{variable}\\s*=\\s*[\"']{re.escape(value)}[\"'](?=\\s|$)",  # LBTESTCD = "PE" (后跟空格或结束)
            f"{variable}\\s*EQ\\s*[\"']{re.escape(value)}[\"'](?=\\s|$)",  # LBTESTCD EQ "PE" (后跟空格或结束)
            f"(?<![A-Za-z0-9]){re.escape(value)}(?![A-Za-z0-9]).*{variable}",  # PE...LBTESTCD (完整单词匹配)
            f"{variable}.*(?<![A-Za-z0-9]){re.escape(value)}(?![A-Za-z0-9])",  # LBTESTCD...PE (完整单词匹配)
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
                break  # 找到一个匹配就停止
    
    return matches

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
        
        if supp_flag == 'Y':
            # SUPP=Y时，搜索QNAM=变量名的格式
            patterns = [
                f"QNAM\\s*=\\s*{re.escape(variable_name)}(?=\\s|$)",  # QNAM=AEACT
                f"QNAM\\s*=\\s*[\"']{re.escape(variable_name)}[\"'](?=\\s|$)",  # QNAM="AEACT"
                f"(?<![A-Za-z0-9]){re.escape(variable_name)}(?![A-Za-z0-9]).*QNAM",  # AEACT...QNAM
                f"QNAM.*(?<![A-Za-z0-9]){re.escape(variable_name)}(?![A-Za-z0-9])",  # QNAM...AEACT
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

# 处理domains变量匹配
print("\n开始处理domains变量匹配...")
domain_results = []

if domains_df is not None and annots_df is not None:
    for idx, row in domains_df.iterrows():
        domain = row.get('Domain', '')
        variable_name = row.get('Variable Name', '')
        supp_flag = row.get('SUPP', 'N')  # 默认为N
        
        if variable_name:
            print(f"\n处理Domain变量: {domain}.{variable_name} (SUPP={supp_flag})")
            
            matches = find_domain_variable_in_annotations(domain, variable_name, supp_flag, annots_df)
            if matches:
                for match in matches:
                    result = {
                        'Type': 'Domain_Variable',
                        'Domain': domain,
                        'Variable': variable_name,
                        'SUPP': supp_flag,
                        'Where_Clause': '',
                        'Parsed_Variable': variable_name,
                        'Parsed_Value': '',
                        'Found_Page': match['page'],
                        'Found_Content': match['content'],
                        'Pattern_Matched': match['pattern_matched']
                    }
                    domain_results.append(result)
                    print(f"    找到匹配: 页码{match['page']}, 内容: {match['content'][:50]}...")
            else:
                print(f"    未找到匹配项")

print(f"\nDomain变量匹配完成，共找到{len(domain_results)}个匹配项")

print("\n开始解析Where Clause并查找页码...")
if vlm_df is not None and annots_df is not None:
    vlm_results = []
    
    for idx, row in vlm_df.iterrows():
        where_clause = row.get('Where Clause', '')
        dataset = row.get('Dataset', '')
        variable_name = row.get('Variable', '')
        
        var, val = parse_where_clause(where_clause)
        if var and val:
            print(f"\n处理: {where_clause}")
            print(f"  解析出变量: {var}, 值: {val}")
            
            matches = find_variable_in_annotations(var, val, annots_df)
            if matches:
                for match in matches:
                    result = {
                        'Type': 'VLM_WhereClause',
                        'Dataset': dataset,
                        'Variable': variable_name,
                        'SUPP': '',
                        'Where_Clause': where_clause,
                        'Parsed_Variable': var,
                        'Parsed_Value': val,
                        'Found_Page': match['page'],
                        'Found_Content': match['content'],
                        'Pattern_Matched': match['pattern_matched']
                    }
                    vlm_results.append(result)
                    print(f"    找到匹配: 页码{match['page']}, 内容: {match['content'][:50]}...")
            else:
                print(f"    未找到匹配项")
    
    # 合并所有结果
    all_results = domain_results + vlm_results
    
    # 输出结果
    if all_results:
        results_df = pd.DataFrame(all_results)
        output_file = r"E:\JAVAPROJ\008_defineXML\Sas\define\项目Spec\variable_page_mapping.xlsx"
        results_df.to_excel(output_file, index=False)
        print(f"\n结果已保存到: {output_file}")
        print(f"共找到{len(all_results)}个匹配项 (Domain变量: {len(domain_results)}, VLM条件: {len(vlm_results)})")
        
        # 显示前几个结果
        print("\n前5个匹配结果:")
        print(results_df.head())
    else:
        print("\n未找到任何匹配项")
else:
    print("缺少必要的数据文件，无法进行匹配")