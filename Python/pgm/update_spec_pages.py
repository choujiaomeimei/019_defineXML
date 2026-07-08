import pandas as pd
import os

# 文件路径
spec_file = r"E:\JAVAPROJ\008_defineXML\Python\output\Spec_out\Annots2.xlsx"
vlm_file = r"E:\JAVAPROJ\008_defineXML\Python\output\Spec_out\vlm_codelists.xlsx"
mapping_file = r"E:\JAVAPROJ\008_defineXML\Python\output\Spec_out\variable_page_mapping.xlsx"

print("读取变量页码映射文件...")
try:
    mapping_df = pd.read_excel(mapping_file)
    print(f"映射文件已读取，共{len(mapping_df)}行")
    print(f"列名: {list(mapping_df.columns)}")
    print(f"前5行:")
    print(mapping_df.head())
except Exception as e:
    print(f"读取映射文件失败: {e}")
    exit()

# 检查spec文件是否可以正常打开
print(f"\n检查spec文件...")
try:
    # 尝试用xlrd引擎读取
    excel_file = pd.ExcelFile(spec_file, engine='xlrd')
    print(f"使用xlrd引擎成功打开spec文件")
except:
    try:
        # 尝试用openpyxl引擎读取
        excel_file = pd.ExcelFile(spec_file, engine='openpyxl')
        print(f"使用openpyxl引擎成功打开spec文件")
    except Exception as e:
        print(f"无法打开spec文件: {e}")
        print("跳过spec文件更新，只更新VLM文件")
        excel_file = None

# 按变量分组，获取页码列表
def get_pages_for_variable(domain, variable, mapping_df):
    """获取指定domain和变量的所有页码"""
    # 筛选匹配的记录
    matches = mapping_df[
        (mapping_df['Domain'] == domain) & 
        (mapping_df['Variable'] == variable)
    ]
    
    if matches.empty:
        return ""
    
    # 获取所有页码，去重并排序
    pages = sorted(list(set(matches['Found_Page'].tolist())))
    # 转换为字符串并用空格连接
    return " ".join(str(p) for p in pages)

def get_pages_for_vlm_variable(dataset, variable, where_clause, mapping_df):
    """获取VLM变量的页码"""
    # 筛选VLM记录
    matches = mapping_df[
        (mapping_df['Type'] == 'VLM_WhereClause') &
        (mapping_df['Dataset'] == dataset) & 
        (mapping_df['Variable'] == variable)
    ]
    
    if not where_clause:
        # 如果没有where clause，使用dataset和variable匹配
        if matches.empty:
            return ""
    else:
        # 如果有where clause，进一步筛选
        matches = matches[matches['Where_Clause'] == where_clause]
        if matches.empty:
            return ""
    
    # 获取所有页码，去重并排序
    pages = sorted(list(set(matches['Found_Page'].tolist())))
    return " ".join(str(p) for p in pages)

# 更新spec.xlsx中的domain表
updated_sheets = {}
total_updates = 0

if excel_file is not None:
    print("\n开始更新spec.xlsx中的domain表...")
    
    # 读取Excel文件
    domains_to_update = ['DM', 'AE', 'PC', 'PP', 'PE', 'LB', 'EG', 'VS']
    
    for domain in domains_to_update:
        if domain in excel_file.sheet_names:
            print(f"\n处理domain: {domain}")
            try:
                df = pd.read_excel(spec_file, sheet_name=domain, engine=excel_file.engine)
                
                if 'Variable Name' in df.columns and 'Origin' in df.columns and 'CRF Page' in df.columns:
                    updates_count = 0
                    
                    for idx, row in df.iterrows():
                        origin = str(row.get('Origin', '')).strip()
                        variable_name = row.get('Variable Name', '')
                        
                        # 只更新Origin=CRF的记录
                        if origin.upper() == 'CRF' and variable_name:
                            pages = get_pages_for_variable(domain, variable_name, mapping_df)
                            if pages:
                                # 确保列的数据类型为字符串
                                df['CRF Page'] = df['CRF Page'].astype(str)
                                df.at[idx, 'CRF Page'] = str(pages)
                                updates_count += 1
                                print(f"  更新 {variable_name}: {pages}")
                    
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
else:
    print("\n跳过spec.xlsx更新")

# 更新VLM文件
print(f"\n开始更新VLM文件...")
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
            
            # 只更新Origin=CRF的记录
            if origin.upper() == 'CRF':
                pages = get_pages_for_vlm_variable(dataset, variable, where_clause, mapping_df)
                if pages:
                    # 确保列的数据类型为字符串
                    vlm_df['Pages'] = vlm_df['Pages'].astype(str)
                    vlm_df.at[idx, 'Pages'] = str(pages)
                    vlm_updates += 1
                    print(f"  更新 {dataset}.{variable}: {pages}")
        
        print(f"VLM文件共更新{vlm_updates}个变量的页码")
        total_updates += vlm_updates
    else:
        print("VLM文件缺少必要的列")
        vlm_df = None
        
except Exception as e:
    print(f"读取VLM文件失败: {e}")
    vlm_df = None

# 保存更新后的文件
print(f"\n开始保存更新后的文件...")

# 保存spec.xlsx
if excel_file is not None and updated_sheets:
    # 读取所有原始工作表
    all_sheets = {}
    try:
        for sheet_name in excel_file.sheet_names:
            if sheet_name in updated_sheets:
                all_sheets[sheet_name] = updated_sheets[sheet_name]
            else:
                all_sheets[sheet_name] = pd.read_excel(spec_file, sheet_name=sheet_name, engine=excel_file.engine)
        
        # 备份原文件
        backup_file = spec_file.replace('.xlsx', '_backup.xlsx')
        import shutil
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
else:
    print("跳过spec.xlsx保存")

# 保存VLM文件
if vlm_df is not None:
    # 备份原文件
    backup_vlm = vlm_file.replace('.xlsx', '_backup.xlsx')
    import shutil
    shutil.copy2(vlm_file, backup_vlm)
    print(f"原VLM文件已备份为: {backup_vlm}")
    
    # 保存更新后的文件
    vlm_df.to_excel(vlm_file, index=False)
    from excel_style import style_excel_file
    style_excel_file(vlm_file)
    print(f"VLM文件已更新并保存")

print(f"\n更新完成！总共更新了{total_updates}个变量的页码信息")