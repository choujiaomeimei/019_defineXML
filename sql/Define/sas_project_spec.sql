-- ========================================
-- 项目Spec数据表
-- 创建时间: 2025-01-10
-- 说明: 存储项目的变量定义信息，支持所有CDISC标准域
-- ========================================

USE define_db;

-- 删除表（如果存在）
DROP TABLE IF EXISTS `sas_project_spec`;

-- 创建项目Spec数据表
CREATE TABLE `sas_project_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Spec数据ID',
  `project_id` varchar(64) NOT NULL COMMENT '项目ID，关联项目',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名，用于权限控制',
  `domain` varchar(20) NOT NULL COMMENT '数据域/数据集名称(如DM, AE, CM等)',
  `variable` varchar(50) NOT NULL COMMENT '变量名称',
  `label` varchar(500) DEFAULT NULL COMMENT '变量标签/描述',
  `type` varchar(20) DEFAULT NULL COMMENT '数据类型(Char, Num, Date等)',
  `length` varchar(50) DEFAULT NULL COMMENT '变量长度（保留原始字符串）',
  `controlled_terms_or_format` varchar(200) DEFAULT NULL COMMENT '受控术语或格式',
  `cdisc_submission_value` varchar(200) DEFAULT NULL COMMENT 'CDISC提交值',
  `decimal_places` int DEFAULT NULL COMMENT '小数位数(仅数值型)',
  `origin` varchar(100) DEFAULT NULL COMMENT '数据来源(CRF, Assigned, Derived等)',
  `role` varchar(50) DEFAULT NULL COMMENT '变量角色(Identifier, Topic, Qualifier等)',
  `cdisc_notes` varchar(1000) DEFAULT NULL COMMENT 'CDISC注释',
  `core` varchar(10) DEFAULT NULL COMMENT '核心级别(Req, Exp, Perm)',
  `codelist` varchar(100) DEFAULT NULL COMMENT '代码列表引用',
  `format` varchar(50) DEFAULT NULL COMMENT '显示格式',
  `comment` text DEFAULT NULL COMMENT '备注说明',
  `mandatory` varchar(10) DEFAULT NULL COMMENT '是否必填(Yes/No)',
  `key_sequence` int DEFAULT NULL COMMENT '主键序号',
  `controlled_terms` varchar(500) DEFAULT NULL COMMENT '受控术语',
  `derivation` text DEFAULT NULL COMMENT '派生逻辑',
  `predecessor` varchar(50) DEFAULT NULL COMMENT '前置变量',
  `method` varchar(200) DEFAULT NULL COMMENT '方法/算法',
  `pages` varchar(200) DEFAULT NULL COMMENT '相关页面',
  `question_text` text DEFAULT NULL COMMENT '问题文本',
  `prompt` varchar(200) DEFAULT NULL COMMENT '提示信息',
  `dataset_class` varchar(50) DEFAULT NULL COMMENT '数据集类别',
  `structure` varchar(50) DEFAULT NULL COMMENT '数据结构',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_domain_variable` (`project_id`, `domain`, `variable`),
  KEY `idx_project_domain` (`project_id`, `domain`),
  KEY `idx_username` (`username`),
  KEY `idx_domain` (`domain`),
  KEY `idx_variable` (`variable`),
  KEY `idx_origin` (`origin`),
  KEY `idx_role` (`role`),
  KEY `idx_core` (`core`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目Spec数据表-存储所有domain的变量定义信息';

-- 验证创建结果
SELECT 'sas_project_spec表创建完成' AS '执行结果';
DESC sas_project_spec;