-- Flyway V3: Project member table for data isolation

CREATE TABLE IF NOT EXISTS `project_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` VARCHAR(50) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `role` ENUM('owner', 'editor', 'viewer') NOT NULL DEFAULT 'editor',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_project_user` (`project_id`, `username`),
    INDEX `idx_username` (`username`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate existing project ownership
INSERT IGNORE INTO `project_member` (`project_id`, `username`, `role`)
SELECT `project_id`, `username`, 'owner'
FROM `project`
WHERE `username` IS NOT NULL AND `username` != '';
