-- ========================================
-- 版本管理与文件统一 - 数据库变更脚本
-- 更新时间: 2026-03-16
-- ========================================

USE define_db;

-- ========================================
-- 1. file_upload_records 表增加 version_number 字段
-- ========================================
ALTER TABLE `file_upload_records`
  ADD COLUMN IF NOT EXISTS `version_number` INT DEFAULT 1 COMMENT '当前版本号(每次替换+1)' AFTER `remarks`;

-- ========================================
-- 2. 新增 file_version_history 表 (文件历史版本)
-- ========================================
CREATE TABLE IF NOT EXISTS `file_version_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `file_id` VARCHAR(100) NOT NULL COMMENT '原文件ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `username` VARCHAR(100) COMMENT '上传用户名',
    `file_category` ENUM('P21_SPEC','XPT','PROJECT_SPEC','ACRF','EDC_CODELIST','VLM','CODELIST') NOT NULL COMMENT '文件类别',
    `version_number` INT NOT NULL COMMENT '版本号(从1递增)',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `server_file_name` VARCHAR(255) NOT NULL COMMENT '服务器存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `file_extension` VARCHAR(10) COMMENT '文件扩展名',
    `file_md5` VARCHAR(32) COMMENT '文件MD5值',
    `upload_time` DATETIME NOT NULL COMMENT '原始上传时间',
    `process_status` VARCHAR(20) COMMENT '归档时的处理状态',
    `replaced_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '被替换的时间',
    `replaced_by` VARCHAR(100) COMMENT '替换操作人',
    INDEX `idx_project_category` (`project_id`, `file_category`),
    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_replaced_time` (`replaced_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件历史版本表-记录被替换的旧文件';

-- ========================================
-- 3. 新增 project_snapshot 表 (项目版本快照)
-- ========================================
CREATE TABLE IF NOT EXISTS `project_snapshot` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `snapshot_name` VARCHAR(200) NOT NULL COMMENT '版本名称(如v1.0-FDA提交)',
    `snapshot_type` ENUM('manual','auto') DEFAULT 'manual' COMMENT '快照类型: manual-手动, auto-自动',
    `description` TEXT COMMENT '版本说明',
    `spec_data_json` LONGTEXT COMMENT 'sas_project_spec完整JSON快照',
    `vlm_data_json` LONGTEXT COMMENT 'sas_vlm_data完整JSON快照',
    `codelist_data_json` LONGTEXT COMMENT 'sas_codelist_data完整JSON快照',
    `config_json` TEXT COMMENT 'project_config快照',
    `created_by` VARCHAR(100) COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_project_id` (`project_id`),
    INDEX `idx_created_time` (`created_time`),
    INDEX `idx_snapshot_type` (`snapshot_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目版本快照表-冻结关键节点的完整数据';

-- ========================================
-- 4. 新增 snapshot_files 表 (快照关联文件)
-- ========================================
CREATE TABLE IF NOT EXISTS `snapshot_files` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `snapshot_id` BIGINT NOT NULL COMMENT '快照ID',
    `file_category` ENUM('P21_SPEC','XPT','PROJECT_SPEC','ACRF','EDC_CODELIST','VLM','CODELIST') NOT NULL COMMENT '文件类别',
    `original_name` VARCHAR(255) COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '快照时的文件路径(冻结副本)',
    `file_size` BIGINT COMMENT '文件大小(字节)',
    `file_md5` VARCHAR(32) COMMENT '文件MD5值',
    `version_number` INT COMMENT '快照时的文件版本号',
    INDEX `idx_snapshot_id` (`snapshot_id`),
    FOREIGN KEY (`snapshot_id`) REFERENCES `project_snapshot`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快照关联文件表-记录快照时各类文件的状态';

-- ========================================
-- 验证
-- ========================================
SELECT '版本管理表创建完成' AS '执行结果';
SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'define_db' AND TABLE_NAME IN ('file_version_history', 'project_snapshot', 'snapshot_files');
