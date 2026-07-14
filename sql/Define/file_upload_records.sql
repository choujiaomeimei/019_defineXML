-- ========================================
-- 统一文件上传记录表 - 替代多个分类上传表
-- 创建时间: 2025-01-10
-- 版本: 1.0 (优化版本 - 合并所有上传文件类型)
-- ========================================

USE define_db;

-- 删除旧的分类上传表
DROP TABLE IF EXISTS `sas_acrf_upload`;
DROP TABLE IF EXISTS `sas_project_spec_upload`;
DROP TABLE IF EXISTS `sas_xpt_upload`;
DROP TABLE IF EXISTS `sas_p21_spec_upload`;

-- 创建统一文件上传记录表
CREATE TABLE IF NOT EXISTS `file_upload_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `file_id` VARCHAR(100) NOT NULL COMMENT '文件唯一标识',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `username` VARCHAR(100) NOT NULL COMMENT '上传用户名',
    `file_category` ENUM('P21_SPEC', 'XPT', 'PROJECT_SPEC', 'ACRF', 'EDC_CODELIST', 'VLM', 'CODELIST') NOT NULL COMMENT '文件类别',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `server_file_name` VARCHAR(255) NOT NULL COMMENT '服务器存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `workspace_file_path` VARCHAR(500) NULL COMMENT 'projects下当前工作副本路径',
    `standard_type` VARCHAR(20) NULL COMMENT '标准类型',
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

    -- 索引设计
    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_project_id` (`project_id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_file_category` (`file_category`),
    INDEX `idx_upload_time` (`upload_time`),
    INDEX `idx_upload_status` (`upload_status`),
    INDEX `idx_process_status` (`process_status`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_project_category` (`project_id`, `file_category`),
    INDEX `idx_process_time` (`process_time`),

    -- 唯一约束：同一项目同一类别的文件名不能重复
    UNIQUE KEY `uk_project_category_name` (`project_id`, `file_category`, `original_name`, `deleted`),

    -- 外键约束已移除，由应用层保证数据一致性
    INDEX `idx_fk_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一文件上传记录表';

-- ========================================
-- 创建文件类别统计视图
-- ========================================
CREATE OR REPLACE VIEW `v_file_upload_stats` AS
SELECT
    `project_id`,
    `file_category`,
    COUNT(*) as `total_files`,
    COUNT(CASE WHEN `upload_status` = 'success' THEN 1 END) as `success_count`,
    COUNT(CASE WHEN `upload_status` = 'failed' THEN 1 END) as `failed_count`,
    COUNT(CASE WHEN `process_status` = 'completed' THEN 1 END) as `processed_count`,
    SUM(`file_size`) as `total_size`,
    MIN(`upload_time`) as `first_upload`,
    MAX(`upload_time`) as `last_upload`
FROM `file_upload_records`
WHERE `deleted` = 0
GROUP BY `project_id`, `file_category`
ORDER BY `project_id`, `file_category`;

-- ========================================
-- 创建项目文件汇总视图
-- ========================================
CREATE OR REPLACE VIEW `v_project_file_summary` AS
SELECT
    `project_id`,
    COUNT(*) as `total_files`,
    COUNT(CASE WHEN `file_category` = 'P21_SPEC' THEN 1 END) as `p21_spec_count`,
    COUNT(CASE WHEN `file_category` = 'XPT' THEN 1 END) as `xpt_count`,
    COUNT(CASE WHEN `file_category` = 'PROJECT_SPEC' THEN 1 END) as `project_spec_count`,
    COUNT(CASE WHEN `file_category` = 'ACRF' THEN 1 END) as `acrf_count`,
    COUNT(CASE WHEN `file_category` = 'EDC_CODELIST' THEN 1 END) as `edc_codelist_count`,
    COUNT(CASE WHEN `file_category` = 'VLM' THEN 1 END) as `vlm_count`,
    COUNT(CASE WHEN `file_category` = 'CODELIST' THEN 1 END) as `codelist_count`,
    SUM(`file_size`) as `total_size_bytes`,
    ROUND(SUM(`file_size`) / 1024 / 1024, 2) as `total_size_mb`
FROM `file_upload_records`
WHERE `deleted` = 0 AND `upload_status` = 'success'
GROUP BY `project_id`
ORDER BY `project_id`;

-- ========================================
-- 插入示例数据（可选）
-- ========================================
-- INSERT INTO `file_upload_records` (
--     `file_id`, `project_id`, `username`, `file_category`,
--     `original_name`, `server_file_name`, `file_path`,
--     `file_size`, `file_extension`, `upload_time`,
--     `upload_status`, `process_status`
-- ) VALUES
-- ('FILE_001', 'P000_Demo', 'demo_user', 'P21_SPEC',
--  'demo_p21_spec.xlsx', '20250110_demo_p21_spec.xlsx',
--  '/projects/P000_Demo/uploads/p21-spec/20250110_demo_p21_spec.xlsx',
--  1024000, 'xlsx', NOW(), 'success', 'completed');

-- ========================================
-- 验证表创建结果
-- ========================================
SHOW CREATE TABLE `file_upload_records`;

SELECT 'file_upload_records表创建完成' AS '执行结果';
SELECT '支持文件类别: P21_SPEC, XPT, PROJECT_SPEC, ACRF, VLM, CODELIST' AS '说明';

-- 验证表结构
DESC `file_upload_records`;

-- 查看索引
SHOW INDEX FROM `file_upload_records`;