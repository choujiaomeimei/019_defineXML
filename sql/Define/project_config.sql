-- ========================================
-- 项目配置表
-- 创建时间: 2025-01-10
-- 说明: 存储项目的标准配置信息，如SDTM版本、CT版本等
-- ========================================

USE define_db;

-- 删除表（如果存在）
DROP TABLE IF EXISTS `project_config`;

-- 创建项目配置表
CREATE TABLE IF NOT EXISTS `project_config` (
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

-- 插入默认配置数据
INSERT IGNORE INTO `project_config` (
    `project_id`,
    `encoding`,
    `language`,
    `standard_type`,
    `standard_version`,
    `ct_version`,
    `chinese_standard`,
    `english_standard`,
    `source_format`,
    `configuration`,
    `creator`
) VALUES
('DEFAULT', 'UTF-8', 'CN', 'SDTM', 'SDTM-IG3.2', 'CT2021-12-17', TRUE, FALSE, 'SAS(XPT)', 'SDTM-IG3.2(NMPA)', 'system'),
('P000_Demo', 'UTF-8', 'CN', 'SDTM', 'SDTM-IG3.2', 'CT2021-12-17', TRUE, FALSE, 'SAS(XPT)', 'SDTM-IG3.2(NMPA)', 'system');

-- 验证创建结果
SELECT 'project_config表创建完成' AS '执行结果';
SELECT COUNT(*) AS '记录数' FROM project_config;