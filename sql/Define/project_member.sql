-- ========================================
-- 项目成员关联表 - 支持多用户协作与数据隔离
-- 更新时间: 2026-03-25
-- ========================================

USE define_db;

CREATE TABLE IF NOT EXISTS `project_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `role` ENUM('owner', 'editor', 'viewer') NOT NULL DEFAULT 'editor' COMMENT '角色: owner-所有者, editor-编辑者, viewer-查看者',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_project_user` (`project_id`, `username`),
    INDEX `idx_username` (`username`),
    INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员关联表';

-- Migrate existing project ownership data
INSERT IGNORE INTO `project_member` (`project_id`, `username`, `role`)
SELECT `project_id`, `username`, 'owner'
FROM `project`
WHERE `username` IS NOT NULL AND `username` != '';

SELECT '项目成员表创建完成' AS '执行结果';
