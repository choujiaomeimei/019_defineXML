-- ========================================
-- SDRG (Study Data Reviewer's Guide) 表
-- 更新时间: 2026-03-25
-- ========================================

USE define_db;

CREATE TABLE IF NOT EXISTS `sdrg_content` (
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
    INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SDRG章节内容表';

CREATE TABLE IF NOT EXISTS `sdrg_template` (
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

SELECT 'SDRG表创建完成' AS '执行结果';
