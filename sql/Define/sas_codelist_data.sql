-- ========================================
-- CodeList代码列表数据表
-- 创建时间: 2025-01-10
-- 说明: 存储代码列表数据，支持多语言和版本管理
-- ========================================

USE define_db;

-- 删除表（如果存在）
DROP TABLE IF EXISTS `sas_codelist_data`;

-- 创建CodeList代码列表数据表
CREATE TABLE `sas_codelist_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'CodeList数据ID',
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID，关联项目',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名，用于权限控制',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CodeList代码列表数据表';

-- 验证创建结果
SELECT 'sas_codelist_data表创建完成' AS '执行结果';
DESC sas_codelist_data;