-- ========================================
-- 修复 file_upload_records 表 - 添加缺失的列
-- ========================================
USE define_db;

ALTER TABLE `file_upload_records`
  ADD COLUMN IF NOT EXISTS `process_duration_ms` INT NULL COMMENT '处理耗时(毫秒)' AFTER `process_time`,
  ADD COLUMN IF NOT EXISTS `version_number` INT DEFAULT 1 COMMENT '当前版本号(每次替换+1)' AFTER `remarks`;

SELECT '修复完成' AS '执行结果';
DESCRIBE file_upload_records;
