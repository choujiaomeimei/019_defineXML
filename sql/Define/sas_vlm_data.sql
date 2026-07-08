-- ========================================
-- VLM变量级元数据表
-- 创建时间: 2025-01-10
-- 说明: 存储变量级元数据，支持复杂的WHERE条件和标签管理
-- ========================================

USE define_db;

-- 删除表（如果存在）
DROP TABLE IF EXISTS `sas_vlm_data`;

-- 创建VLM变量级元数据表
CREATE TABLE `sas_vlm_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'VLM数据ID',
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID，关联项目',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名，用于权限控制',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VLM变量级元数据表';

-- 验证创建结果
SELECT 'sas_vlm_data表创建完成' AS '执行结果';
DESC sas_vlm_data;