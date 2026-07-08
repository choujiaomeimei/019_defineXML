#!/usr/bin/env python3
"""
重构sas_project_spec表结构 - 将两个字段放在length后面
开发阶段直接修改表结构
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

def backup_and_recreate_table(connection):
    """备份数据并重建表结构"""
    cursor = connection.cursor()
    
    try:
        # 1. 备份现有数据
        print("1. 备份现有数据...")
        cursor.execute("""
        CREATE TABLE sas_project_spec_backup AS 
        SELECT * FROM sas_project_spec
        """)
        
        backup_count = cursor.rowcount
        print(f"   备份了 {backup_count} 条记录")
        
        # 2. 删除原表
        print("2. 删除原表...")
        cursor.execute("DROP TABLE sas_project_spec")
        
        # 3. 创建新表结构（将新字段放在length后面）
        print("3. 创建新表结构...")
        create_table_sql = """
        CREATE TABLE `sas_project_spec` (
          `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
          `project_id` varchar(64) NOT NULL COMMENT '项目ID，关联项目',
          `domain` varchar(20) NOT NULL COMMENT '域名称(如DM, AE, LB等)',
          `variable` varchar(50) NOT NULL COMMENT '变量名称',
          `label` varchar(500) DEFAULT NULL COMMENT '变量标签/描述',
          `type` varchar(20) DEFAULT NULL COMMENT '数据类型(如Char, Num等)',
          `length` int(11) DEFAULT NULL COMMENT '变量长度',
          `controlled_terms_or_format` varchar(200) DEFAULT NULL COMMENT '受控术语或格式',
          `cdisc_submission_value` varchar(200) DEFAULT NULL COMMENT 'CDISC提交值',
          `decimal_places` int(11) DEFAULT NULL COMMENT '小数位数',
          `origin` varchar(100) DEFAULT NULL COMMENT '数据来源(如CRF, Assigned等)',
          `role` varchar(50) DEFAULT NULL COMMENT '变量角色',
          `cdisc_notes` varchar(1000) DEFAULT NULL COMMENT 'CDISC注释',
          `core` varchar(10) DEFAULT NULL COMMENT '核心状态',
          `codelist` varchar(100) DEFAULT NULL COMMENT '代码列表',
          `format` varchar(50) DEFAULT NULL COMMENT '显示格式',
          `comment` text COMMENT '备注信息',
          `mandatory` tinyint(1) DEFAULT 0 COMMENT '是否必填',
          `key_sequence` int(11) DEFAULT NULL COMMENT '主键序号',
          `controlled_terms` varchar(500) DEFAULT NULL COMMENT '受控术语',
          `derivation` text COMMENT '派生逻辑',
          `predecessor` varchar(50) DEFAULT NULL COMMENT '前置变量',
          `method` varchar(200) DEFAULT NULL COMMENT '方法',
          `pages` varchar(200) DEFAULT NULL COMMENT 'CRF页面',
          `question_text` text COMMENT '问题文本',
          `prompt` varchar(200) DEFAULT NULL COMMENT '提示信息',
          `dataset_class` varchar(50) DEFAULT NULL COMMENT '数据集类别',
          `structure` varchar(50) DEFAULT NULL COMMENT '数据结构',
          `sort_order` int(11) DEFAULT 0 COMMENT '排序顺序',
          `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
          `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
          `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
          `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
          PRIMARY KEY (`id`),
          KEY `idx_project_domain` (`project_id`,`domain`),
          KEY `idx_domain_variable` (`domain`,`variable`),
          KEY `idx_origin` (`origin`),
          KEY `idx_role` (`role`),
          KEY `idx_core` (`core`),
          KEY `idx_created_time` (`created_time`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SAS项目规范变量表'
        """
        
        cursor.execute(create_table_sql)
        print("   新表结构创建成功")
        
        # 4. 恢复数据
        print("4. 恢复数据...")
        
        # 获取备份表的所有字段
        cursor.execute("DESCRIBE sas_project_spec_backup")
        backup_columns = [row[0] for row in cursor.fetchall()]
        
        # 获取新表的所有字段
        cursor.execute("DESCRIBE sas_project_spec")
        new_columns = [row[0] for row in cursor.fetchall()]
        
        # 找到共同字段
        common_columns = [col for col in backup_columns if col in new_columns]
        columns_str = ', '.join(f"`{col}`" for col in common_columns)
        
        restore_sql = f"""
        INSERT INTO sas_project_spec ({columns_str})
        SELECT {columns_str} FROM sas_project_spec_backup
        """
        
        cursor.execute(restore_sql)
        restored_count = cursor.rowcount
        print(f"   恢复了 {restored_count} 条记录")
        
        # 5. 删除备份表
        print("5. 删除备份表...")
        cursor.execute("DROP TABLE sas_project_spec_backup")
        
        connection.commit()
        print("表结构重构完成！")
        
        return True
        
    except Exception as e:
        print(f"重构失败: {e}")
        connection.rollback()
        
        # 尝试从备份恢复
        try:
            print("尝试从备份恢复...")
            cursor.execute("DROP TABLE IF EXISTS sas_project_spec")
            cursor.execute("RENAME TABLE sas_project_spec_backup TO sas_project_spec")
            connection.commit()
            print("已从备份恢复原表")
        except:
            print("备份恢复也失败，请手动检查数据库")
        
        return False
    
    finally:
        cursor.close()

def verify_new_structure(connection):
    """验证新表结构"""
    cursor = connection.cursor()
    
    try:
        cursor.execute("DESCRIBE sas_project_spec")
        columns = cursor.fetchall()
        
        print("\n新表结构:")
        print(f"{'Field':<35} {'Type':<20} {'Null':<5} {'Comment'}")
        print("-" * 80)
        
        target_fields = ['controlled_terms_or_format', 'cdisc_submission_value']
        found_fields = []
        
        for i, column in enumerate(columns):
            field, type_info, null, key, default, extra = column
            print(f"{field:<35} {type_info:<20} {null:<5}")
            
            if field in target_fields:
                found_fields.append(field)
                
            # 检查字段位置
            if field == 'length' and i < len(columns) - 1:
                next_field = columns[i + 1][0]
                if next_field == 'controlled_terms_or_format':
                    print(f"   ✓ controlled_terms_or_format 正确位于 length 后面")
        
        print(f"\n字段总数: {len(columns)}")
        print(f"目标字段检查:")
        for field in target_fields:
            if field in found_fields:
                print(f"   ✓ {field} - 存在")
            else:
                print(f"   ✗ {field} - 缺失")
        
        # 检查数据数量
        cursor.execute("SELECT COUNT(*) FROM sas_project_spec")
        count = cursor.fetchone()[0]
        print(f"数据记录数: {count}")
        
    except Exception as e:
        print(f"验证失败: {e}")
    finally:
        cursor.close()

def main():
    """主函数"""
    print("=== 重构sas_project_spec表结构 ===")
    print("将controlled_terms_or_format和cdisc_submission_value字段移到length后面")
    
    # 开发阶段直接执行
    print("\n开发阶段，直接执行表结构重构...")
    
    # 连接数据库
    connection = connect_database()
    if not connection:
        sys.exit(1)
    
    try:
        # 重构表结构
        if backup_and_recreate_table(connection):
            # 验证结果
            verify_new_structure(connection)
        else:
            print("重构失败")
            sys.exit(1)
            
    except Exception as e:
        print(f"操作失败: {e}")
        sys.exit(1)
    finally:
        connection.close()

if __name__ == "__main__":
    main()