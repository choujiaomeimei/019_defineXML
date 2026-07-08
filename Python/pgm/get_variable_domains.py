#!/usr/bin/env python3
"""
获取CDISC Submission Value对应的Domain列表
例如：ARM -> [TA,DM], USUBJID -> [DM,AE,LB,...]
"""

import pymysql
import pandas as pd
import json
import sys

def connect_database():
    """连接数据库"""
    try:
        connection = pymysql.connect(
            host='localhost',
            user='root', 
            password='root',
            database='crf',
            charset='utf8mb4'
        )
        return connection
    except Exception as e:
        print(f"数据库连接失败: {e}")
        return None

def get_cdisc_variable_domains():
    """获取CDISC Submission Value对应的域列表"""
    connection = connect_database()
    if not connection:
        return {}
    
    cursor = connection.cursor()
    
    try:
        # 查询所有有CDISC Submission Value的变量及其所在domain
        sql = """
        SELECT 
            cdisc_submission_value,
            GROUP_CONCAT(DISTINCT domain ORDER BY domain) as domains
        FROM sas_project_spec 
        WHERE cdisc_submission_value IS NOT NULL 
          AND cdisc_submission_value != ''
          AND cdisc_submission_value != 'NULL'
        GROUP BY cdisc_submission_value
        ORDER BY cdisc_submission_value
        """
        
        cursor.execute(sql)
        results = cursor.fetchall()
        
        variable_domains = {}
        
        print("CDISC Submission Value -> Domain映射:")
        print("-" * 50)
        
        for result in results:
            cdisc_value, domains_str = result
            if domains_str:
                domain_list = [d.strip() for d in domains_str.split(',')]
                variable_domains[cdisc_value] = domain_list
                domain_display = '[' + ','.join(domain_list) + ']'
                print(f"{cdisc_value:<15} -> {domain_display}")
        
        return variable_domains
        
    except Exception as e:
        print(f"查询失败: {e}")
        return {}
    finally:
        cursor.close()
        connection.close()

def get_variable_usage_stats():
    """获取变量使用统计信息"""
    connection = connect_database()
    if not connection:
        return []
    
    cursor = connection.cursor()
    
    try:
        # 详细统计每个变量的使用情况
        sql = """
        SELECT 
            COALESCE(cdisc_submission_value, variable) as display_variable,
            cdisc_submission_value,
            variable as original_variable,
            COUNT(DISTINCT domain) as domain_count,
            GROUP_CONCAT(DISTINCT domain ORDER BY domain) as domains,
            COUNT(*) as total_occurrences
        FROM sas_project_spec 
        WHERE variable IS NOT NULL AND variable != ''
        GROUP BY COALESCE(cdisc_submission_value, variable), cdisc_submission_value, variable
        HAVING domain_count > 1  -- 只显示在多个domain中出现的变量
        ORDER BY domain_count DESC, display_variable
        """
        
        cursor.execute(sql)
        results = cursor.fetchall()
        
        stats = []
        
        print(f"\n变量跨域使用统计 (在多个Domain中出现的变量):")
        print("-" * 80)
        print(f"{'变量名':<15} {'CDISC值':<15} {'域数量':<8} {'出现域':<30} {'总次数':<8}")
        print("-" * 80)
        
        for result in results:
            display_var, cdisc_val, orig_var, domain_count, domains_str, total_count = result
            
            domain_list = [d.strip() for d in domains_str.split(',') if d.strip()] if domains_str else []
            domain_display = '[' + ','.join(domain_list) + ']'
            
            stat_info = {
                'variable': display_var,
                'cdisc_submission_value': cdisc_val or '',
                'original_variable': orig_var,
                'domain_count': domain_count,
                'domains': domain_list,
                'domain_display': domain_display,
                'total_occurrences': total_count
            }
            
            stats.append(stat_info)
            
            cdisc_display = cdisc_val if cdisc_val else '(无)'
            print(f"{display_var:<15} {cdisc_display:<15} {domain_count:<8} {domain_display:<30} {total_count:<8}")
        
        return stats
        
    except Exception as e:
        print(f"统计查询失败: {e}")
        return []
    finally:
        cursor.close()
        connection.close()

def generate_json_mapping():
    """生成JSON格式的映射数据供前端使用"""
    variable_domains = get_cdisc_variable_domains()
    
    if not variable_domains:
        print("没有找到CDISC Submission Value数据")
        return
    
    # 生成前端可用的JSON数据
    json_output = {
        'variable_domain_mapping': variable_domains,
        'total_variables': len(variable_domains),
        'generated_time': pd.Timestamp.now().strftime('%Y-%m-%d %H:%M:%S')
    }
    
    # 保存到文件
    output_file = r"E:\JAVAPROJ\008_defineXML\前端源码\online-typing-website\src\data\variable_domain_mapping.json"
    
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(json_output, f, ensure_ascii=False, indent=2)
        
        print(f"\nJSON映射文件已生成: {output_file}")
        print(f"包含 {len(variable_domains)} 个变量的域映射")
        
        # 输出前端可直接使用的JavaScript代码
        js_code = f"""
// 变量Domain映射 - 自动生成于 {json_output['generated_time']}
export const variableDomainMapping = {json.dumps(variable_domains, ensure_ascii=False, indent=2)};

// 获取变量所在的Domain列表
export function getVariableDomains(cdiscSubmissionValue) {{
  return variableDomainMapping[cdiscSubmissionValue] || [];
}}

// 格式化显示Domain列表
export function formatDomainDisplay(cdiscSubmissionValue) {{
  const domains = getVariableDomains(cdiscSubmissionValue);
  return domains.length > 0 ? '[' + domains.join(',') + ']' : '';
}}
"""
        
        js_file = r"E:\JAVAPROJ\008_defineXML\前端源码\online-typing-website\src\utils\variableDomainMapping.js"
        with open(js_file, 'w', encoding='utf-8') as f:
            f.write(js_code)
        
        print(f"JavaScript工具文件已生成: {js_file}")
        
    except Exception as e:
        print(f"生成文件失败: {e}")

def main():
    """主函数"""
    print("=== CDISC Submission Value域映射分析 ===")
    
    # 1. 获取基本映射
    variable_domains = get_cdisc_variable_domains()
    
    # 2. 获取详细统计
    stats = get_variable_usage_stats()
    
    # 3. 生成前端可用的文件
    generate_json_mapping()
    
    # 4. 输出一些示例
    print(f"\n=== 使用示例 ===")
    examples = ['ARM', 'USUBJID', 'STUDYID', 'SUBJID']
    for example in examples:
        if example in variable_domains:
            domains = variable_domains[example]
            domain_display = '[' + ','.join(domains) + ']'
            print(f"{example} 在 {domain_display} 中")

if __name__ == "__main__":
    main()