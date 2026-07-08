-- Flyway V2: Version management tables

ALTER TABLE `file_upload_records`
  ADD COLUMN IF NOT EXISTS `version_number` INT DEFAULT 1 COMMENT '当前版本号(每次替换+1)';

CREATE TABLE IF NOT EXISTS `file_version_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `file_id` VARCHAR(100) NOT NULL,
    `project_id` VARCHAR(50) NOT NULL,
    `username` VARCHAR(100),
    `file_category` ENUM('P21_SPEC','XPT','PROJECT_SPEC','ACRF','VLM','CODELIST') NOT NULL,
    `version_number` INT NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `server_file_name` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(500) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `file_extension` VARCHAR(10),
    `file_md5` VARCHAR(32),
    `upload_time` DATETIME NOT NULL,
    `process_status` VARCHAR(20),
    `replaced_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `replaced_by` VARCHAR(100),
    INDEX `idx_project_category` (`project_id`, `file_category`),
    INDEX `idx_file_id` (`file_id`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `project_snapshot` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` VARCHAR(50) NOT NULL,
    `snapshot_name` VARCHAR(200) NOT NULL,
    `snapshot_type` ENUM('manual','auto') DEFAULT 'manual',
    `description` TEXT,
    `spec_data_json` LONGTEXT,
    `vlm_data_json` LONGTEXT,
    `codelist_data_json` LONGTEXT,
    `config_json` TEXT,
    `locked` TINYINT(1) DEFAULT 0,
    `version_label` VARCHAR(50) DEFAULT NULL,
    `created_by` VARCHAR(100),
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_project_id` (`project_id`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `snapshot_files` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `snapshot_id` BIGINT NOT NULL,
    `file_category` ENUM('P21_SPEC','XPT','PROJECT_SPEC','ACRF','VLM','CODELIST') NOT NULL,
    `original_name` VARCHAR(255),
    `file_path` VARCHAR(500) NOT NULL,
    `file_size` BIGINT,
    `file_md5` VARCHAR(32),
    `version_number` INT,
    INDEX `idx_snapshot_id` (`snapshot_id`),
    FOREIGN KEY (`snapshot_id`) REFERENCES `project_snapshot`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
