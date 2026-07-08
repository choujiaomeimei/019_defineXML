-- ========================================
-- Define XML 项目完整数据库表创建脚本
-- 更新时间: 2026-03-27
-- 版本: 3.0 (完整版，包含所有业务表)
-- ========================================

CREATE DATABASE IF NOT EXISTS define_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE define_db;

-- ========================================
-- 按依赖顺序删除所有表
-- ========================================
DROP TABLE IF EXISTS `snapshot_files`;
DROP TABLE IF EXISTS `project_snapshot`;
DROP TABLE IF EXISTS `file_version_history`;
DROP TABLE IF EXISTS `file_upload_records`;
DROP TABLE IF EXISTS `sdrg_content`;
DROP TABLE IF EXISTS `sdrg_template`;
DROP TABLE IF EXISTS `define_sheet_data`;
DROP TABLE IF EXISTS `sas_pages_data`;
DROP TABLE IF EXISTS `sas_datasets_data`;
DROP TABLE IF EXISTS `sas_comments_data`;
DROP TABLE IF EXISTS `sas_methods_data`;
DROP TABLE IF EXISTS `sas_codelist_data`;
DROP TABLE IF EXISTS `sas_vlm_data`;
DROP TABLE IF EXISTS `sas_project_spec`;
DROP TABLE IF EXISTS `project_member`;
DROP TABLE IF EXISTS `project_config`;
DROP TABLE IF EXISTS `project`;
DROP TABLE IF EXISTS `user`;

-- ========================================
-- 1. 用户表
-- ========================================
CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `email` VARCHAR(200) COMMENT '邮箱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================
-- 2. 项目管理表 (主表)
-- ========================================
CREATE TABLE `project` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `project_name` VARCHAR(200) COMMENT '项目名称',
    `protocol_number` VARCHAR(100) COMMENT '方案编号',
    `protocol_name` VARCHAR(200) COMMENT '方案名称',
    `protocol_version` VARCHAR(20) COMMENT '方案版本',
    `protocol_date` DATE COMMENT '方案版本日期',
    `sponsor` VARCHAR(200) COMMENT '申办方/赞助方',
    `standard_type` VARCHAR(50) DEFAULT 'SDTM' COMMENT '标准类型(SDTM,ADAM,SEND)',
    `username` VARCHAR(100) COMMENT '用户名',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：1-活跃，0-归档',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_id` (`project_id`),
    KEY `idx_protocol_number` (`protocol_number`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_created_time` (`created_time`),
    KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目管理表';

-- ========================================
-- 3. 项目配置表
-- ========================================
CREATE TABLE `project_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID(关联project表)',
    `encoding` VARCHAR(20) DEFAULT 'UTF-8' COMMENT '使用环境编码',
    `language` VARCHAR(10) DEFAULT 'CN' COMMENT '使用语言(CN/EN)',
    `standard_type` VARCHAR(20) NOT NULL COMMENT '标准类型(SEND/SDTM/ADAM)',
    `standard_version` VARCHAR(20) COMMENT '标准版本(如SDTM-IG3.2)',
    `ct_version` VARCHAR(20) COMMENT 'CT版本',
    `chinese_standard` BOOLEAN DEFAULT TRUE COMMENT '中文标准',
    `english_standard` BOOLEAN DEFAULT FALSE COMMENT '英文标准',
    `source_format` VARCHAR(50) DEFAULT 'SAS(XPT)' COMMENT '源文件格式',
    `configuration` VARCHAR(100) COMMENT '配置信息',
    `creator` VARCHAR(50) COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_project_id` (`project_id`),
    INDEX `idx_standard_type` (`standard_type`),
    INDEX `idx_creator` (`creator`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目配置表';

-- ========================================
-- 4. 项目成员表
-- ========================================
CREATE TABLE `project_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `role` ENUM('owner', 'editor', 'viewer') NOT NULL DEFAULT 'editor' COMMENT '角色',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_project_user` (`project_id`, `username`),
    INDEX `idx_username` (`username`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

-- ========================================
-- 5. 项目Spec数据表
-- ========================================
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
  `text_content` text DEFAULT NULL COMMENT 'Text内容(原Derived Method)',
  `method` varchar(200) DEFAULT NULL COMMENT 'Method标识',
  `pages` text DEFAULT NULL COMMENT '相关页面',
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

-- ========================================
-- 6. VLM变量级元数据表
-- ========================================
CREATE TABLE `sas_vlm_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'VLM数据ID',
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID，关联项目',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名，用于权限控制',
  `dataset` varchar(50) NOT NULL COMMENT '数据集名称(如TS, LB, VS等)',
  `variable` varchar(100) NOT NULL COMMENT '变量名(如TSVAL, LBORRES等)',
  `where_clause` varchar(500) DEFAULT NULL COMMENT 'WHERE条件子句',
  `label` varchar(500) DEFAULT NULL COMMENT '变量标签/描述',
  `data_type` varchar(20) DEFAULT NULL COMMENT '数据类型',
  `length` varchar(50) DEFAULT NULL COMMENT '变量长度',
  `significant_digits` varchar(20) DEFAULT NULL COMMENT '有效位数',
  `format` varchar(50) DEFAULT NULL COMMENT '显示格式',
  `mandatory` varchar(10) DEFAULT 'No' COMMENT '是否必填',
  `assigned_value` varchar(200) DEFAULT NULL COMMENT '指定值',
  `codelist` varchar(100) DEFAULT NULL COMMENT '代码列表',
  `controlled_terms_or_format` varchar(200) DEFAULT NULL COMMENT '受控术语或格式',
  `origin` varchar(50) DEFAULT NULL COMMENT '数据来源(如CRF, Assigned等)',
  `source` varchar(200) DEFAULT NULL COMMENT '数据Source',
  `pages` text DEFAULT NULL COMMENT '页面信息',
  `derivation_comment` text COMMENT '派生/注释信息',
  `method` varchar(10) DEFAULT NULL COMMENT '方法标识',
  `predecessor` varchar(50) DEFAULT NULL COMMENT '前置变量',
  `comment` text COMMENT '备注',
  `developer_notes` text DEFAULT NULL COMMENT '开发者注释',
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

-- ========================================
-- 7. CodeList代码列表数据表
-- ========================================
CREATE TABLE `sas_codelist_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'CodeList数据ID',
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `vcd` varchar(100) NOT NULL COMMENT 'ID: 来自Variables Submission Value或VLM Codelist',
  `vlabel` varchar(500) DEFAULT NULL COMMENT 'Name: 变量标签',
  `nci_codelist_code` varchar(100) DEFAULT NULL COMMENT 'NCI Codelist Code: CT中的codelist_code',
  `type` varchar(20) DEFAULT 'Char' COMMENT 'Data Type: 数据类型',
  `terminology` varchar(200) DEFAULT NULL COMMENT 'Terminology: CT版本如SDTM Terminology 2023-06-30',
  `comment` text DEFAULT NULL COMMENT 'Comment备注',
  `cdnum` int DEFAULT NULL COMMENT 'Order: 代码序号',
  `code` varchar(200) DEFAULT NULL COMMENT 'Term: 代码值',
  `nci_term_code` varchar(100) DEFAULT NULL COMMENT 'NCI Term Code: CT中的term_code',
  `code_des` text DEFAULT NULL COMMENT 'Decoded Value: 代码描述/定义',
  `code_ver` varchar(50) DEFAULT NULL COMMENT '代码版本(保留)',
  `flag` varchar(10) DEFAULT NULL COMMENT '标记字段(保留)',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CodeList代码列表数据表';

-- ========================================
-- 7b. Codelist删除记录追踪表
-- ========================================
CREATE TABLE IF NOT EXISTS `sas_codelist_deleted` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `project_id` VARCHAR(64) DEFAULT NULL COMMENT '项目ID',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  `vcd` VARCHAR(200) DEFAULT NULL COMMENT '被删除的Codelist ID',
  `deleted_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间',
  UNIQUE KEY `uk_project_user_vcd` (`project_id`, `username`, `vcd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Codelist删除记录追踪表';

-- ========================================
-- 7c. Codelist合并记录追踪表
-- ========================================
CREATE TABLE IF NOT EXISTS `sas_codelist_merge_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `project_id` VARCHAR(64) DEFAULT NULL COMMENT '项目ID',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  `original_vcd` VARCHAR(200) DEFAULT NULL COMMENT '合并前的 vcd',
  `merged_vcd` VARCHAR(200) DEFAULT NULL COMMENT '合并后的 vcd',
  `merged_vlabel` VARCHAR(500) DEFAULT NULL COMMENT '合并后的 name',
  `merged_nci_code` VARCHAR(100) DEFAULT NULL COMMENT '合并后的 NCI Codelist Code',
  `merge_batch_id` VARCHAR(64) DEFAULT NULL COMMENT '同次批量合并的 batch id（用于按原候选组分组显示）',
  `merge_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '合并时间',
  KEY `idx_project` (`project_id`, `username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Codelist合并记录追踪表';

-- 7d. 合并候选组序号持久化表
CREATE TABLE IF NOT EXISTS `sas_cluster_group_no` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `project_id` VARCHAR(64) DEFAULT NULL COMMENT '项目ID',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  `cluster_key` VARCHAR(255) DEFAULT NULL COMMENT 'fp:<fingerprint>(identity)、sc:<supersetFingerprint>(subset_chain) 或 bt:<merge_batch_id>(已合并)',
  `group_no` INT DEFAULT NULL COMMENT '稳定的候选组序号',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次分配时间',
  UNIQUE KEY `uk_pcuk` (`project_id`, `username`, `cluster_key`),
  KEY `idx_groupno` (`project_id`, `username`, `group_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合并候选组序号持久化表（保证刷新/合并/撤销后序号不变）';

-- 8b. Codelist合并引用追踪表（用于撤销时精确还原原始引用）
CREATE TABLE IF NOT EXISTS `sas_codelist_merge_ref_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `project_id` VARCHAR(64) DEFAULT NULL COMMENT '项目ID',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  `merged_vcd` VARCHAR(200) DEFAULT NULL COMMENT '合并后的 vcd',
  `ref_type` VARCHAR(20) DEFAULT NULL COMMENT '引用表类型: spec 或 vlm',
  `ref_row_id` BIGINT DEFAULT NULL COMMENT '被更新行的 id',
  `original_codelist` VARCHAR(200) DEFAULT NULL COMMENT '该行合并前的 codelist 值',
  `merge_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '合并时间',
  KEY `idx_lookup` (`project_id`, `username`, `merged_vcd`, `ref_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Codelist合并引用追踪表（撤销用）';

-- 8c. Codelist合并 term 快照表（撤销时精确还原原始 term 行）
CREATE TABLE IF NOT EXISTS `sas_codelist_merge_term_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `project_id` VARCHAR(64) DEFAULT NULL COMMENT '项目ID',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  `merged_vcd` VARCHAR(200) DEFAULT NULL COMMENT '合并后的 vcd',
  `original_vcd` VARCHAR(200) DEFAULT NULL COMMENT '合并前的原始 vcd',
  `snap_vlabel` VARCHAR(500) DEFAULT NULL,
  `snap_nci_codelist_code` VARCHAR(100) DEFAULT NULL,
  `snap_type` VARCHAR(20) DEFAULT NULL,
  `snap_terminology` VARCHAR(200) DEFAULT NULL,
  `snap_comment` TEXT,
  `snap_cdnum` INT DEFAULT NULL,
  `snap_code` VARCHAR(200) DEFAULT NULL,
  `snap_nci_term_code` VARCHAR(100) DEFAULT NULL,
  `snap_code_des` TEXT,
  `snap_code_ver` VARCHAR(50) DEFAULT NULL,
  `snap_flag` VARCHAR(10) DEFAULT NULL,
  `snap_sort_order` INT DEFAULT NULL,
  `snap_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_lookup` (`project_id`, `username`, `merged_vcd`, `original_vcd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Codelist合并 term 行快照表（撤销用）';

-- ========================================
-- 8a. Methods方法数据表
-- ========================================
CREATE TABLE `sas_methods_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` varchar(64) NOT NULL COMMENT '项目ID',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `method_id` varchar(200) NOT NULL COMMENT 'Method ID (domain.variable)',
  `name` varchar(500) DEFAULT NULL COMMENT 'Method名称',
  `type` varchar(100) DEFAULT NULL COMMENT 'Method类型',
  `description` text DEFAULT NULL COMMENT 'Method描述',
  `expression_context` varchar(200) DEFAULT NULL COMMENT '表达式上下文',
  `expression_code` text DEFAULT NULL COMMENT '表达式代码',
  `document` varchar(500) DEFAULT NULL COMMENT '相关文档',
  `pages` text DEFAULT NULL COMMENT '相关页面',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_username` (`username`),
  KEY `idx_method_id` (`method_id`),
  KEY `idx_project_method` (`project_id`, `method_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Methods方法数据表';

-- ========================================
-- 8b. Comments注释数据表
-- ========================================
CREATE TABLE `sas_comments_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` varchar(64) NOT NULL COMMENT '项目ID',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `comment_id` varchar(200) NOT NULL COMMENT 'Comment ID (domain.variable)',
  `description` text DEFAULT NULL COMMENT 'Comment描述',
  `document` varchar(500) DEFAULT NULL COMMENT '相关文档',
  `pages` text DEFAULT NULL COMMENT '相关页面',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_username` (`username`),
  KEY `idx_comment_id` (`comment_id`),
  KEY `idx_project_comment` (`project_id`, `comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Comments注释数据表';

-- ========================================
-- 8c. Datasets数据集定义表 (P21标准数据集定义)
-- ========================================
CREATE TABLE `sas_datasets_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` varchar(64) NOT NULL COMMENT '项目ID',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `dataset` varchar(50) NOT NULL COMMENT '数据集名称',
  `label` varchar(500) DEFAULT NULL COMMENT '数据集标签',
  `class` varchar(50) DEFAULT NULL COMMENT '数据集分类',
  `sub_class` varchar(50) DEFAULT NULL COMMENT '数据集子分类',
  `structure` varchar(50) DEFAULT NULL COMMENT '数据结构',
  `key_variables` varchar(500) DEFAULT NULL COMMENT '关键变量',
  `standard` varchar(100) DEFAULT NULL COMMENT '标准引用',
  `has_no_data` varchar(10) DEFAULT 'No' COMMENT '是否无数据(Yes/No)',
  `repeating` varchar(10) DEFAULT NULL COMMENT '是否重复(Yes/No)',
  `reference_data` varchar(10) DEFAULT NULL COMMENT '是否参考数据(Yes/No)',
  `comment` text DEFAULT NULL COMMENT '备注',
  `developer_notes` text DEFAULT NULL COMMENT '开发者注释',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_username` (`username`),
  KEY `idx_project_dataset` (`project_id`, `dataset`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Datasets数据表-P21标准数据集定义';

-- ========================================
-- 8d. Pages数据表 (aCRF页码提取结果)
-- ========================================
CREATE TABLE `sas_pages_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` varchar(64) NOT NULL COMMENT '项目ID',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `dataset` varchar(50) NOT NULL COMMENT '数据集名称',
  `variable` varchar(100) NOT NULL COMMENT '变量名',
  `where_clause` varchar(500) DEFAULT NULL COMMENT 'VLM筛选条件',
  `pages` varchar(200) DEFAULT NULL COMMENT '页码信息',
  `origin` varchar(50) DEFAULT NULL COMMENT '数据来源: Domain_Variable / VLM_WhereClause',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_project_dataset` (`project_id`, `dataset`),
  KEY `idx_dataset_variable` (`dataset`, `variable`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pages数据表-aCRF页码提取结果';

-- ========================================
-- 9. 统一文件上传记录表
-- ========================================
CREATE TABLE `file_upload_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `file_id` VARCHAR(100) NOT NULL COMMENT '文件唯一标识',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `username` VARCHAR(100) NOT NULL COMMENT '上传用户名',
    `file_category` ENUM('P21_SPEC', 'XPT', 'PROJECT_SPEC', 'ACRF', 'VLM', 'CODELIST') NOT NULL COMMENT '文件类别',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `server_file_name` VARCHAR(255) NOT NULL COMMENT '服务器存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `file_extension` VARCHAR(10) NOT NULL COMMENT '文件扩展名',
    `file_md5` VARCHAR(32) COMMENT '文件MD5值',
    `upload_time` DATETIME NOT NULL COMMENT '上传时间',
    `upload_status` ENUM('uploading', 'success', 'failed') DEFAULT 'uploading' COMMENT '上传状态',
    `process_status` ENUM('pending', 'processing', 'completed', 'failed') DEFAULT 'pending' COMMENT '处理状态',
    `process_time` DATETIME NULL COMMENT '处理时间',
    `process_duration_ms` INT NULL COMMENT '处理耗时(毫秒)',
    `output_file_path` VARCHAR(500) NULL COMMENT '处理结果文件路径',
    `error_message` TEXT NULL COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标志: 0-正常, 1-已删除',
    `remarks` TEXT NULL COMMENT '备注信息',
    `version_number` INT DEFAULT 1 COMMENT '当前版本号(每次替换+1)',

    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_project_id` (`project_id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_file_category` (`file_category`),
    INDEX `idx_upload_time` (`upload_time`),
    INDEX `idx_upload_status` (`upload_status`),
    INDEX `idx_process_status` (`process_status`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_project_category` (`project_id`, `file_category`),
    UNIQUE KEY `uk_project_category_name` (`project_id`, `file_category`, `original_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一文件上传记录表';

-- ========================================
-- 10. 文件版本历史表
-- ========================================
CREATE TABLE `file_version_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `file_id` VARCHAR(100) NOT NULL COMMENT '文件唯一标识',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `username` VARCHAR(100) COMMENT '上传用户名',
    `file_category` ENUM('P21_SPEC','XPT','PROJECT_SPEC','ACRF','VLM','CODELIST') NOT NULL COMMENT '文件类别',
    `version_number` INT NOT NULL COMMENT '版本号',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `server_file_name` VARCHAR(255) NOT NULL COMMENT '服务器存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `file_extension` VARCHAR(10) COMMENT '文件扩展名',
    `file_md5` VARCHAR(32) COMMENT '文件MD5值',
    `upload_time` DATETIME NOT NULL COMMENT '上传时间',
    `process_status` VARCHAR(20) COMMENT '处理状态',
    `replaced_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '被替换时间',
    `replaced_by` VARCHAR(100) COMMENT '替换操作人',
    INDEX `idx_project_category` (`project_id`, `file_category`),
    INDEX `idx_file_id` (`file_id`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件版本历史表';

-- ========================================
-- 11. 项目快照表
-- ========================================
CREATE TABLE `project_snapshot` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `snapshot_name` VARCHAR(200) NOT NULL COMMENT '快照名称',
    `snapshot_type` ENUM('manual','auto') DEFAULT 'manual' COMMENT '快照类型',
    `description` TEXT COMMENT '快照描述',
    `spec_data_json` LONGTEXT COMMENT 'Spec数据快照(JSON)',
    `vlm_data_json` LONGTEXT COMMENT 'VLM数据快照(JSON)',
    `codelist_data_json` LONGTEXT COMMENT 'CodeList数据快照(JSON)',
    `config_json` TEXT COMMENT '配置快照(JSON)',
    `locked` TINYINT(1) DEFAULT 0 COMMENT '是否锁定',
    `version_label` VARCHAR(50) DEFAULT NULL COMMENT '版本标签',
    `created_by` VARCHAR(100) COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_project_id` (`project_id`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目快照表';

-- ========================================
-- 12. 快照文件表
-- ========================================
CREATE TABLE `snapshot_files` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `snapshot_id` BIGINT NOT NULL COMMENT '快照ID',
    `file_category` ENUM('P21_SPEC','XPT','PROJECT_SPEC','ACRF','VLM','CODELIST') NOT NULL COMMENT '文件类别',
    `original_name` VARCHAR(255) COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size` BIGINT COMMENT '文件大小(字节)',
    `file_md5` VARCHAR(32) COMMENT '文件MD5值',
    `version_number` INT COMMENT '文件版本号',
    INDEX `idx_snapshot_id` (`snapshot_id`),
    FOREIGN KEY (`snapshot_id`) REFERENCES `project_snapshot`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快照文件表';

-- ========================================
-- 13. SDRG章节内容表
-- ========================================
CREATE TABLE `sdrg_content` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `section_key` VARCHAR(50) NOT NULL COMMENT '章节标识(如 study_info, datasets_overview, deviations)',
    `section_title` VARCHAR(200) NOT NULL COMMENT '章节标题',
    `section_order` INT NOT NULL DEFAULT 0 COMMENT '章节排序',
    `content_text` LONGTEXT COMMENT '章节内容(富文本HTML)',
    `content_json` TEXT COMMENT '结构化数据(JSON格式,用于模板填充)',
    `created_by` VARCHAR(100) COMMENT '创建人',
    `updated_by` VARCHAR(100) COMMENT '更新人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_project_section` (`project_id`, `section_key`),
    INDEX `idx_project_id` (`project_id`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SDRG章节内容表';

-- ========================================
-- 14. SDRG Word模板表
-- ========================================
CREATE TABLE `sdrg_template` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `template_name` VARCHAR(200) NOT NULL COMMENT '模板名称',
    `template_type` VARCHAR(50) NOT NULL DEFAULT 'SDTM' COMMENT '模板类型(SDTM/ADaM/SEND)',
    `file_path` VARCHAR(500) NOT NULL COMMENT '模板文件路径(.docx)',
    `sections_config` TEXT COMMENT '章节配置(JSON: 定义模板中的占位符与section_key的映射)',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认模板',
    `created_by` VARCHAR(100) COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_template_type` (`template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SDRG Word模板表';

-- ========================================
-- 15. Define Sheet数据表
-- ========================================
CREATE TABLE `define_sheet_data` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
    `sheet_data` LONGTEXT NOT NULL COMMENT 'Sheet数据(JSON)',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_project_file` (`project_id`, `file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Define Sheet数据表';

-- ========================================
-- 创建视图 - 项目域统计
-- ========================================
CREATE OR REPLACE VIEW `v_project_spec_domains` AS
SELECT
  `project_id`,
  `domain`,
  COUNT(*) as `variable_count`,
  COUNT(CASE WHEN `core` = 'Req' THEN 1 END) as `required_count`,
  COUNT(CASE WHEN `core` = 'Exp' THEN 1 END) as `expected_count`,
  COUNT(CASE WHEN `core` = 'Perm' THEN 1 END) as `permissible_count`,
  MIN(`created_time`) as `first_created`,
  MAX(`updated_time`) as `last_updated`
FROM `sas_project_spec`
GROUP BY `project_id`, `domain`
ORDER BY `project_id`, `domain`;

-- ========================================
-- 创建视图 - 变量使用频率统计
-- ========================================
CREATE OR REPLACE VIEW `v_variable_usage_stats` AS
SELECT
  `variable`,
  `label`,
  `type`,
  COUNT(DISTINCT `project_id`) as `project_count`,
  COUNT(DISTINCT `domain`) as `domain_count`,
  GROUP_CONCAT(DISTINCT `domain` ORDER BY `domain`) as `used_in_domains`,
  COUNT(*) as `total_usage`
FROM `sas_project_spec`
GROUP BY `variable`, `label`, `type`
HAVING COUNT(*) > 1
ORDER BY `total_usage` DESC, `variable`;

-- ========================================
-- 插入默认数据
-- ========================================

INSERT INTO `user` (`username`, `password`, `email`)
VALUES ('admin', '$2a$10$defaultHashedPasswordPlaceholder', 'admin@system.local');

INSERT INTO `project` (`project_id`, `project_name`, `username`, `status`, `standard_type`, `created_time`, `updated_time`, `deleted`)
VALUES
  ('DEFAULT', '默认项目', 'admin', 1, 'SDTM', NOW(), NOW(), 0),
  ('P000_Demo', '模板项目', 'admin', 1, 'SDTM,ADAM', NOW(), NOW(), 0);

INSERT INTO `project_config` (`project_id`, `encoding`, `language`, `standard_type`, `standard_version`, `ct_version`, `chinese_standard`, `english_standard`, `source_format`, `configuration`, `creator`)
VALUES
  ('DEFAULT', 'UTF-8', 'CN', 'SDTM', 'SDTM-IG3.2', 'CT2021-12-17', TRUE, FALSE, 'SAS(XPT)', 'SDTM-IG3.2(NMPA)', 'admin'),
  ('P000_Demo', 'UTF-8', 'CN', 'SDTM', 'SDTM-IG3.2', 'CT2021-12-17', TRUE, FALSE, 'SAS(XPT)', 'SDTM-IG3.2(NMPA)', 'admin');

INSERT INTO `project_member` (`project_id`, `username`, `role`)
VALUES
  ('DEFAULT', 'admin', 'owner'),
  ('P000_Demo', 'admin', 'owner');

-- ========================================
-- Dictionaries 外部字典引用表
-- ========================================
CREATE TABLE IF NOT EXISTS `sas_dictionaries_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `dictionary_id` varchar(200) DEFAULT NULL COMMENT '字典ID',
  `name` varchar(500) DEFAULT NULL COMMENT '字典名称',
  `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型',
  `dictionary` varchar(200) DEFAULT NULL COMMENT '字典来源',
  `version` varchar(100) DEFAULT NULL COMMENT '版本',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部字典引用表';

-- ========================================
-- Documents 文档引用表
-- ========================================
CREATE TABLE IF NOT EXISTS `sas_documents_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `document_id` varchar(200) DEFAULT NULL COMMENT '文档ID',
  `title` varchar(500) DEFAULT NULL COMMENT '文档标题',
  `href` varchar(500) DEFAULT NULL COMMENT '文档链接',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档引用表';

-- ========================================
-- 执行完成
-- ========================================
SELECT '========================================' AS '';
SELECT 'Define XML 数据库表创建完成!' AS '执行结果';
SELECT '版本3.1 - 完整版(18张表 + 2个视图)' AS '更新说明';
SELECT '========================================' AS '';

SELECT '项目表' AS '表名', COUNT(*) AS '记录数' FROM project
UNION ALL SELECT '项目配置', COUNT(*) FROM project_config
UNION ALL SELECT '项目成员', COUNT(*) FROM project_member
UNION ALL SELECT '用户', COUNT(*) FROM `user`
UNION ALL SELECT 'Spec数据', COUNT(*) FROM sas_project_spec
UNION ALL SELECT 'VLM数据', COUNT(*) FROM sas_vlm_data
UNION ALL SELECT 'CodeList', COUNT(*) FROM sas_codelist_data
UNION ALL SELECT 'Methods', COUNT(*) FROM sas_methods_data
UNION ALL SELECT 'Comments', COUNT(*) FROM sas_comments_data
UNION ALL SELECT 'Datasets', COUNT(*) FROM sas_datasets_data
UNION ALL SELECT 'Pages数据', COUNT(*) FROM sas_pages_data
UNION ALL SELECT 'DefineSheet', COUNT(*) FROM define_sheet_data
UNION ALL SELECT 'SDRG内容', COUNT(*) FROM sdrg_content
UNION ALL SELECT 'SDRG模板', COUNT(*) FROM sdrg_template
UNION ALL SELECT '文件上传记录', COUNT(*) FROM file_upload_records
UNION ALL SELECT '文件历史版本', COUNT(*) FROM file_version_history
UNION ALL SELECT '项目快照', COUNT(*) FROM project_snapshot
UNION ALL SELECT '快照文件', COUNT(*) FROM snapshot_files;
