-- Flyway V5: SDRG tables

CREATE TABLE IF NOT EXISTS `sdrg_content` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` VARCHAR(50) NOT NULL,
    `section_key` VARCHAR(50) NOT NULL,
    `section_title` VARCHAR(200) NOT NULL,
    `section_order` INT NOT NULL DEFAULT 0,
    `content_text` LONGTEXT,
    `content_json` TEXT,
    `created_by` VARCHAR(100),
    `updated_by` VARCHAR(100),
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_project_section` (`project_id`, `section_key`),
    INDEX `idx_project_id` (`project_id`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sdrg_template` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `template_name` VARCHAR(200) NOT NULL,
    `template_type` VARCHAR(50) NOT NULL DEFAULT 'SDTM',
    `file_path` VARCHAR(500) NOT NULL,
    `sections_config` TEXT,
    `is_default` TINYINT(1) DEFAULT 0,
    `created_by` VARCHAR(100),
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_template_type` (`template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
