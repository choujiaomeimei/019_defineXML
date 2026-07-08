#!/usr/bin/env python3
"""
验证表结构
"""

import pymysql
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

def verify_table_structure(connection):
    """验证表结构"""
    cursor = connection.cursor()
    
    try:
        cursor.execute("DESCRIBE sas_project_spec")
        columns = cursor.fetchall()
        
        print("sas_project_spec表结构:")
        print("序号 | 字段名 | 类型 | 是否为空")
        print("-" * 60)
        
        for i, column in enumerate(columns, 1):
            field, type_info, null, key, default, extra = column
            print(f"{i:2d}   | {field:<30} | {type_info:<15} | {null}")
        
        print(f"\n总字段数: {len(columns)}")
        
        # 检查关键字段位置
        field_names = [col[0] for col in columns]
        
        if 'length' in field_names and 'controlled_terms_or_format' in field_names:
            length_idx = field_names.index('length')
            controlled_idx = field_names.index('controlled_terms_or_format')
            
            print(f"\nlength字段位置: {length_idx + 1}")
            print(f"controlled_terms_or_format字段位置: {controlled_idx + 1}")
            
            if controlled_idx == length_idx + 1:
                print("OK: controlled_terms_or_format 紧跟在 length 后面")
            else:
                print("WARNING: 字段位置不符合预期")
        
        if 'cdisc_submission_value' in field_names:
            cdisc_idx = field_names.index('cdisc_submission_value') 
            print(f"cdisc_submission_value字段位置: {cdisc_idx + 1}")
        
        # 检查数据量
        cursor.execute("SELECT COUNT(*) FROM sas_project_spec")
        count = cursor.fetchone()[0]
        print(f"\n数据记录总数: {count}")
        
        return True
        
    except Exception as e:
        print(f"验证失败: {e}")
        return False
    finally:
        cursor.close()

def main():
    """主函数"""
    connection = connect_database()
    if not connection:
        sys.exit(1)
    
    try:
        verify_table_structure(connection)
    finally:
        connection.close()

if __name__ == "__main__":
    main()