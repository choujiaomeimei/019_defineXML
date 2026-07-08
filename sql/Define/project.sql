-- ========================================
-- 项目管理表
-- 创建时间: 2025-01-10
-- 说明: 项目管理主表，存储项目基本信息
-- ========================================

USE define_db;

-- 删除表（如果存在）
DROP TABLE IF EXISTS `project`;

-- 创建项目管理表
CREATE TABLE `project` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `project_name` VARCHAR(200) COMMENT '项目名称',
    `protocol_number` VARCHAR(100) COMMENT '方案编号',
    `protocol_name` VARCHAR(200) COMMENT '方案名称',
    `protocol_version` VARCHAR(20) COMMENT '方案版本',
    `protocol_date` DATE COMMENT '方案版本日期',
    `standard_type` VARCHAR(20) COMMENT '标准类型：SDTM、ADAM、SEND',
    `sponsor` VARCHAR(200) COMMENT '申办方/赞助方',
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

-- 插入默认数据
INSERT IGNORE INTO `project` (`project_id`, `project_name`, `username`, `status`, `created_time`, `updated_time`, `deleted`, `standard_type`)
VALUES ('DEFAULT', '默认项目', 'system', 1, NOW(), NOW(), 0, 'SDTM');

INSERT IGNORE INTO `project` (`project_id`, `project_name`, `username`, `status`, `created_time`, `updated_time`, `deleted`, `standard_type`)
VALUES ('P000_Demo', '模板项目', 'system', 1, NOW(), NOW(), 0, 'SDTM,ADAM');

-- 验证创建结果
SELECT 'project表创建完成' AS '执行结果';
SELECT COUNT(*) AS '记录数' FROM project;