#!/usr/bin/env python3
"""
修复sas_project_spec表结构 - 添加缺失的字段
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
            charset='utf8mb4',
            autocommit=False
        )
        return connection
    except Exception as e:
        print(f"数据库连接失败: {e}")
        return None

def check_table_structure(connection):
    """检查表结构"""
    try:
        cursor = connection.cursor()
        cursor.execute("DESCRIBE sas_project_spec")
        columns = cursor.fetchall()
        
        print("当前sas_project_spec表结构:")
        print(f"{'Field':<30} {'Type':<20} {'Null':<5} {'Key':<5} {'Default':<10} {'Extra':<10}")
        print("-" * 80)
        
        existing_columns = []
        for column in columns:
            field, type_info, null, key, default, extra = column
            existing_columns.append(field)
            print(f"{field:<30} {type_info:<20} {null:<5} {key:<5} {str(default):<10} {extra:<10}")
        
        cursor.close()
        return existing_columns
        
    except Exception as e:
        print(f"检查表结构失败: {e}")
        return []

def add_missing_columns(connection, existing_columns):
    """添加缺失的字段"""
    cursor = connection.cursor()
    
    # 需要添加的字段
    columns_to_add = [
        {
            'name': 'controlled_terms_or_format',
            'sql': "ADD COLUMN `controlled_terms_or_format` VARCHAR(200) DEFAULT NULL COMMENT '受控术语或格式'"
        },
        {
            'name': 'cdisc_submission_value', 
            'sql': "ADD COLUMN `cdisc_submission_value` VARCHAR(200) DEFAULT NULL COMMENT 'CDISC提交值'"
        }
    ]
    
    added_count = 0
    
    for column_info in columns_to_add:
        column_name = column_info['name']
        
        # 检查字段是否已存在
        if column_name in existing_columns:
            print(f"字段 {column_name} 已存在，跳过")
            continue
            
        try:
            # 添加字段
            alter_sql = f"ALTER TABLE sas_project_spec {column_info['sql']}"
            print(f"执行SQL: {alter_sql}")
            
            cursor.execute(alter_sql)
            connection.commit()
            
            print(f"OK 成功添加字段: {column_name}")
            added_count += 1
            
        except Exception as e:
            print(f"ERROR 添加字段 {column_name} 失败: {e}")
            connection.rollback()
    
    cursor.close()
    return added_count

def verify_changes(connection):
    """验证修改结果"""
    print("\n验证修改结果:")
    updated_columns = check_table_structure(connection)
    
    # 检查目标字段是否存在
    target_fields = ['controlled_terms_or_format', 'cdisc_submission_value']
    
    for field in target_fields:
        if field in updated_columns:
            print(f"OK {field} 字段已存在")
        else:
            print(f"ERROR {field} 字段仍然缺失")

def main():
    """主函数"""
    print("=== 修复sas_project_spec表结构 ===")
    
    # 连接数据库
    connection = connect_database()
    if not connection:
        sys.exit(1)
    
    try:
        # 检查当前表结构
        existing_columns = check_table_structure(connection)
        if not existing_columns:
            print("无法获取表结构")
            sys.exit(1)
        
        print(f"\n表中现有字段数量: {len(existing_columns)}")
        
        # 添加缺失字段
        print("\n开始添加缺失字段...")
        added_count = add_missing_columns(connection, existing_columns)
        
        # 验证修改
        verify_changes(connection)
        
        print(f"\n修复完成！共添加了 {added_count} 个字段")
        
    except Exception as e:
        print(f"修复过程出错: {e}")
        sys.exit(1)
    finally:
        connection.close()

if __name__ == "__main__":
    main()