-- ========================================
-- 版本管理增强 - 快照锁定/冻结 + 版本号
-- 更新时间: 2026-03-25
-- ========================================

USE define_db;

ALTER TABLE `project_snapshot`
  ADD COLUMN IF NOT EXISTS `locked` TINYINT(1) DEFAULT 0 COMMENT '是否锁定: 0-未锁定, 1-已锁定(冻结)',
  ADD COLUMN IF NOT EXISTS `version_label` VARCHAR(50) DEFAULT NULL COMMENT '语义版本号(如 v1.0, v2.0-FDA)';

SELECT '版本管理增强完成' AS '执行结果';
