#!/usr/bin/env python3
"""
VLM and CodeList Database Manager
将提取的VLM和CodeList数据导入到数据库中，并提供管理功能
"""

import os
import pandas as pd
import numpy as np
import pymysql
from pathlib import Path
from typing import Dict, List, Optional
import warnings
import json
from datetime import datetime

warnings.filterwarnings('ignore')


class VLMCodeListDBManager:
    """VLM和CodeList数据库管理器"""
    
    def __init__(self, db_config: Dict[str, str], project_id: str = "default", username: str = None):
        """
        初始化数据库管理器
        
        Args:
            db_config: 数据库配置字典，包含host, user, password, database等
            project_id: 项目ID，用于区分不同项目的数据
            username: 用户名，用于数据隔离
        """
        self.db_config = db_config
        self.project_id = project_id
        self.username = username or os.environ.get('USERNAME_CONTEXT', '')
        self.connection = None
        
    def connect_db(self):
        """连接数据库"""
        try:
            self.connection = pymysql.connect(
                host=self.db_config['host'],
                user=self.db_config['user'],
                password=self.db_config['password'],
                database=self.db_config['database'],
                charset='utf8mb4',
                autocommit=True
            )
            return True
        except Exception as e:
            print(f"数据库连接失败: {e}")
            return False
    
    def close_db(self):
        """关闭数据库连接"""
        if self.connection:
            self.connection.close()
            self.connection = None
    
    def create_tables_if_not_exists(self):
        """创建数据表（如果不存在）"""
        if not self.connection:
            print("数据库未连接")
            return False
            
        cursor = self.connection.cursor()
        
        try:
            # 创建VLM表
            vlm_sql = """
            CREATE TABLE IF NOT EXISTS `sas_vlm_data` (
              `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'VLM数据ID',
              `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID，关联项目',
              `username` varchar(100) DEFAULT NULL COMMENT '用户名，数据隔离',
              `dataset` varchar(50) NOT NULL COMMENT '数据集名称(如TS, LB, VS等)',
              `variable` varchar(100) NOT NULL COMMENT '变量名(如TSVAL, LBORRES等)',
              `where_clause` varchar(500) DEFAULT NULL COMMENT 'WHERE条件子句',
              `label` varchar(500) DEFAULT NULL COMMENT '变量标签/描述',
              `controlled_terms_or_format` varchar(200) DEFAULT NULL COMMENT '受控术语或格式',
              `origin` varchar(50) DEFAULT NULL COMMENT '数据来源(如CRF, Assigned等)',
              `pages` varchar(200) DEFAULT NULL COMMENT '页面信息',
              `derivation_comment` text COMMENT '派生/注释信息',
              `method` varchar(10) DEFAULT NULL COMMENT '方法标识',
              `comment` text COMMENT '备注',
              `category` varchar(100) DEFAULT NULL COMMENT '类别',
              `sort_order` int DEFAULT '0' COMMENT '排序顺序',
              `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
              `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`id`),
              KEY `idx_project_dataset` (`project_id`,`dataset`),
              KEY `idx_username` (`username`),
              KEY `idx_dataset_variable` (`dataset`,`variable`),
              KEY `idx_sort_order` (`sort_order`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VLM变量级元数据表'
            """
            
            cursor.execute(vlm_sql)
            
            # 创建CodeList表
            codelist_sql = """
            CREATE TABLE IF NOT EXISTS `sas_codelist_data` (
              `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'CodeList数据ID',
              `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID，关联项目',
              `username` varchar(100) DEFAULT NULL COMMENT '用户名，数据隔离',
              `vcd` varchar(100) NOT NULL COMMENT '变量代码(如LBTESTCD, VSTEST等)',
              `vlabel` varchar(500) DEFAULT NULL COMMENT '变量标签',
              `type` varchar(20) DEFAULT 'Char' COMMENT '数据类型(Char, Num等)',
              `cdnum` int DEFAULT NULL COMMENT '代码序号',
              `code` varchar(200) DEFAULT NULL COMMENT '代码值',
              `code_des` varchar(500) DEFAULT NULL COMMENT '代码描述',
              `code_ver` varchar(50) DEFAULT NULL COMMENT '代码版本',
              `flag` varchar(10) DEFAULT NULL COMMENT '标记字段',
              `sort_order` int DEFAULT '0' COMMENT '排序顺序',
              `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
              `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`id`),
              KEY `idx_project_vcd` (`project_id`,`vcd`),
              KEY `idx_username` (`username`),
              KEY `idx_vcd_cdnum` (`vcd`,`cdnum`),
              KEY `idx_sort_order` (`sort_order`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CodeList代码列表数据表'
            """
            
            cursor.execute(codelist_sql)
            cursor.close()
            
            print("数据表创建成功")
            return True
            
        except Exception as e:
            print(f"创建数据表失败: {e}")
            cursor.close()
            return False
    
    def clear_project_data(self):
        """清除项目相关的旧数据（只清除本流程产生的codelist，保留Variables级数据）"""
        if not self.connection:
            print("数据库未连接")
            return False
            
        cursor = self.connection.cursor()
        
        try:
            if self.username:
                cursor.execute("DELETE FROM sas_vlm_data WHERE project_id = %s AND username = %s", (self.project_id, self.username))
            else:
                cursor.execute("DELETE FROM sas_vlm_data WHERE project_id = %s", (self.project_id,))
            vlm_deleted = cursor.rowcount
            
            if self.username:
                cursor.execute("DELETE FROM sas_codelist_data WHERE project_id = %s AND username = %s AND created_by = 'vlm_extractor'", (self.project_id, self.username))
            else:
                cursor.execute("DELETE FROM sas_codelist_data WHERE project_id = %s AND created_by = 'vlm_extractor'", (self.project_id,))
            codelist_deleted = cursor.rowcount
            
            cursor.close()
            
            print(f"已清除项目 {self.project_id} (用户: {self.username or 'all'}) 的旧数据: VLM {vlm_deleted} 条, CodeList {codelist_deleted} 条")
            return True
            
        except Exception as e:
            print(f"清除旧数据失败: {e}")
            cursor.close()
            return False
    
    def import_vlm_data(self, vlm_df: pd.DataFrame, created_by: str = "system"):
        """导入VLM数据到数据库"""
        if not self.connection:
            print("数据库未连接")
            return False
            
        if vlm_df.empty:
            print("VLM数据为空")
            return False
            
        cursor = self.connection.cursor()
        
        try:
            insert_sql = """
            INSERT INTO sas_vlm_data (
                project_id, username, dataset, variable, where_clause, label,
                data_type, length, significant_digits, format, mandatory,
                assigned_value, codelist, controlled_terms_or_format, origin,
                source, pages, derivation_comment, method, predecessor,
                comment, developer_notes, category, sort_order, created_by
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            """
            
            dataset_counters = {}
            for index, row in vlm_df.iterrows():
                ds = str(row.get('Dataset', '')).upper()
                dataset_counters[ds] = dataset_counters.get(ds, 0) + 1
                values = (
                    self.project_id,
                    self.username,
                    row.get('Dataset', ''),
                    row.get('Variable', ''),
                    row.get('Where Clause', ''),
                    row.get('Label', ''),
                    row.get('Data Type', None),
                    row.get('Length', None),
                    row.get('Significant Digits', None),
                    row.get('Format', None),
                    'No',
                    None,
                    row.get('Codelist', None),
                    row.get('Controlled Terms or Format', ''),
                    row.get('Origin', ''),
                    row.get('Source', None),
                    row.get('Pages', ''),
                    row.get('Derivation/Comment', ''),
                    row.get('Method', ''),
                    None,
                    row.get('Comment', ''),
                    None,
                    row.get('类别', ''),
                    dataset_counters[ds],
                    created_by
                )
                
                # 处理None值
                values = tuple(None if pd.isna(v) or str(v) == 'nan' else str(v) if v is not None else None for v in values)
                cursor.execute(insert_sql, values)
            
            cursor.close()
            print(f"成功导入 {len(vlm_df)} 条VLM数据")
            return True
            
        except Exception as e:
            print(f"导入VLM数据失败: {e}")
            cursor.close()
            return False
    
    def import_codelist_data(self, codelist_df: pd.DataFrame, created_by: str = "system"):
        """导入CodeList数据到数据库"""
        if not self.connection:
            print("数据库未连接")
            return False
            
        if codelist_df.empty:
            print("CodeList数据为空")
            return False
            
        cursor = self.connection.cursor()
        
        try:
            insert_sql = """
            INSERT INTO sas_codelist_data (
                project_id, username, vcd, vlabel, type, cdnum, code, 
                code_des, code_ver, flag, sort_order, created_by
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            """
            
            for index, row in codelist_df.iterrows():
                values = (
                    self.project_id,
                    self.username,
                    row.get('vcd', ''),
                    row.get('vlabel', ''),
                    row.get('type', 'Char'),
                    row.get('cdnum', 0),
                    row.get('code', ''),
                    row.get('codeDes', ''),
                    row.get('codever', ''),
                    row.get('flag', ''),
                    index,
                    created_by
                )
                
                # 处理None值和数据类型 (索引: 0=project_id,1=username,2=vcd,3=vlabel,4=type,5=cdnum,6=code,7=codeDes,8=codever,9=flag,10=sort_order,11=created_by)
                processed_values = []
                for i, v in enumerate(values):
                    if i == 5:  # cdnum字段 (int)
                        try:
                            processed_values.append(int(v) if v and str(v) != 'nan' else 0)
                        except (ValueError, TypeError):
                            processed_values.append(0)
                    elif i == 10:  # sort_order字段 (int)
                        try:
                            processed_values.append(int(v))
                        except (ValueError, TypeError):
                            processed_values.append(0)
                    else:
                        processed_values.append(None if pd.isna(v) or str(v) == 'nan' else str(v) if v is not None else None)
                
                cursor.execute(insert_sql, tuple(processed_values))
            
            cursor.close()
            print(f"成功导入 {len(codelist_df)} 条CodeList数据")
            return True
            
        except Exception as e:
            print(f"导入CodeList数据失败: {e}")
            cursor.close()
            return False
    
    def get_vlm_data(self, dataset: str = None) -> pd.DataFrame:
        """从数据库获取VLM数据"""
        if not self.connection:
            print("数据库未连接")
            return pd.DataFrame()
            
        cursor = self.connection.cursor()
        
        try:
            if dataset:
                sql = """
                SELECT id, dataset, variable, where_clause, label, 
                       controlled_terms_or_format, origin, pages, derivation_comment,
                       method, comment, category, sort_order
                FROM sas_vlm_data 
                WHERE project_id = %s AND dataset = %s 
                ORDER BY sort_order
                """
                cursor.execute(sql, (self.project_id, dataset))
            else:
                sql = """
                SELECT id, dataset, variable, where_clause, label, 
                       controlled_terms_or_format, origin, pages, derivation_comment,
                       method, comment, category, sort_order
                FROM sas_vlm_data 
                WHERE project_id = %s 
                ORDER BY sort_order
                """
                cursor.execute(sql, (self.project_id,))
            
            results = cursor.fetchall()
            cursor.close()
            
            if not results:
                return pd.DataFrame()
                
            columns = ['id', 'Dataset', 'Variable', 'Where Clause', 'Label', 
                      'Controlled Terms or Format', 'Origin', 'Pages', 'Derivation/Comment',
                      'Method', 'Comment', '类别', 'sort_order']
            
            return pd.DataFrame(results, columns=columns)
            
        except Exception as e:
            print(f"获取VLM数据失败: {e}")
            cursor.close()
            return pd.DataFrame()
    
    def get_codelist_data(self, vcd: str = None) -> pd.DataFrame:
        """从数据库获取CodeList数据"""
        if not self.connection:
            print("数据库未连接")
            return pd.DataFrame()
            
        cursor = self.connection.cursor()
        
        try:
            if vcd:
                sql = """
                SELECT id, vcd, vlabel, type, cdnum, code, code_des, code_ver, flag, sort_order
                FROM sas_codelist_data 
                WHERE project_id = %s AND vcd = %s 
                ORDER BY sort_order
                """
                cursor.execute(sql, (self.project_id, vcd))
            else:
                sql = """
                SELECT id, vcd, vlabel, type, cdnum, code, code_des, code_ver, flag, sort_order
                FROM sas_codelist_data 
                WHERE project_id = %s 
                ORDER BY sort_order
                """
                cursor.execute(sql, (self.project_id,))
            
            results = cursor.fetchall()
            cursor.close()
            
            if not results:
                return pd.DataFrame()
                
            columns = ['id', 'vcd', 'vlabel', 'type', 'cdnum', 'code', 'codeDes', 'codever', 'flag', 'sort_order']
            
            return pd.DataFrame(results, columns=columns)
            
        except Exception as e:
            print(f"获取CodeList数据失败: {e}")
            cursor.close()
            return pd.DataFrame()
    
    def update_sort_order(self, table_name: str, id_order_list: List[Dict[str, int]]):
        """更新数据的排序顺序"""
        if not self.connection:
            print("数据库未连接")
            return False
            
        cursor = self.connection.cursor()
        
        try:
            for item in id_order_list:
                sql = f"UPDATE {table_name} SET sort_order = %s WHERE id = %s"
                cursor.execute(sql, (item['sort_order'], item['id']))
            
            cursor.close()
            print(f"成功更新 {len(id_order_list)} 条记录的排序顺序")
            return True
            
        except Exception as e:
            print(f"更新排序顺序失败: {e}")
            cursor.close()
            return False
    
    def update_vlm_record(self, record_id: int, updates: Dict[str, str]):
        """更新单条VLM记录"""
        if not self.connection:
            print("数据库未连接")
            return False
            
        cursor = self.connection.cursor()
        
        try:
            # 构建UPDATE SQL
            set_clause = ", ".join([f"{key} = %s" for key in updates.keys()])
            sql = f"UPDATE vlm_data SET {set_clause}, updated_time = NOW() WHERE id = %s"
            
            values = list(updates.values()) + [record_id]
            cursor.execute(sql, values)
            
            cursor.close()
            print(f"成功更新VLM记录 ID: {record_id}")
            return True
            
        except Exception as e:
            print(f"更新VLM记录失败: {e}")
            cursor.close()
            return False
    
    def update_codelist_record(self, record_id: int, updates: Dict[str, str]):
        """更新单条CodeList记录"""
        if not self.connection:
            print("数据库未连接")
            return False
            
        cursor = self.connection.cursor()
        
        try:
            # 构建UPDATE SQL
            set_clause = ", ".join([f"{key} = %s" for key in updates.keys()])
            sql = f"UPDATE codelist_data SET {set_clause}, updated_time = NOW() WHERE id = %s"
            
            values = list(updates.values()) + [record_id]
            cursor.execute(sql, values)
            
            cursor.close()
            print(f"成功更新CodeList记录 ID: {record_id}")
            return True
            
        except Exception as e:
            print(f"更新CodeList记录失败: {e}")
            cursor.close()
            return False


def main():
    """主函数示例"""
    # 数据库配置（请根据实际情况修改）
    db_config = {
        'host': 'localhost',
        'user': 'root',
        'password': 'your_password',
        'database': 'define_db'
    }
    
    project_id = "MJR-MR001-01"  # 项目ID
    
    # 创建数据库管理器
    db_manager = VLMCodeListDBManager(db_config, project_id)
    
    try:
        # 连接数据库
        if not db_manager.connect_db():
            return
            
        # 创建表
        if not db_manager.create_tables_if_not_exists():
            return
            
        # 清除旧数据
        db_manager.clear_project_data()
        
        # 读取Excel数据并导入
        excel_path = r"E:\JAVAPROJ\008_defineXML\Python\define\项目Spec\out\vlm_codelists.xlsx"
        
        if Path(excel_path).exists():
            # 导入VLM数据
            vlm_df = pd.read_excel(excel_path, sheet_name='VLM')
            db_manager.import_vlm_data(vlm_df, created_by="vlm_extractor")
            
            # 导入CodeList数据
            codelist_df = pd.read_excel(excel_path, sheet_name='codelists')
            db_manager.import_codelist_data(codelist_df, created_by="vlm_extractor")
            
            print("数据导入完成！")
        else:
            print(f"Excel文件不存在: {excel_path}")
            
    except Exception as e:
        print(f"操作失败: {e}")
    finally:
        # 关闭数据库连接
        db_manager.close_db()


if __name__ == "__main__":
    main()