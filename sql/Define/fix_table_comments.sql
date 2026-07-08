-- ========================================
-- 补全所有数据表的 COMMENT 注释
-- 执行时间: 2026-04-10
-- 用于修复建表时未设置注释的表
-- ========================================
USE define_db;

ALTER TABLE `define_sheet_data`    COMMENT = 'Define Sheet数据表';
ALTER TABLE `file_upload_records`  COMMENT = '统一文件上传记录表';
ALTER TABLE `file_version_history` COMMENT = '文件版本历史表';
ALTER TABLE `project`              COMMENT = '项目管理表';
ALTER TABLE `project_config`       COMMENT = '项目配置表';
ALTER TABLE `project_member`       COMMENT = '项目成员表';
ALTER TABLE `project_snapshot`     COMMENT = '项目版本快照表';
ALTER TABLE `sas_codelist_data`    COMMENT = 'CodeList代码列表数据表';
ALTER TABLE `sas_comments_data`    COMMENT = 'Comments注释数据表';
ALTER TABLE `sas_datasets_data`    COMMENT = 'Datasets数据表-P21标准数据集定义';
ALTER TABLE `sas_methods_data`     COMMENT = 'Methods方法数据表';
ALTER TABLE `sas_pages_data`       COMMENT = 'Pages数据表-aCRF页码提取结果';
ALTER TABLE `sas_project_spec`     COMMENT = '项目Spec数据表-存储所有domain的变量定义信息';
ALTER TABLE `sas_vlm_data`         COMMENT = 'VLM变量级元数据表';
ALTER TABLE `sdrg_content`         COMMENT = 'SDRG章节内容表';
ALTER TABLE `sdrg_template`        COMMENT = 'SDRG Word模板表';
ALTER TABLE `snapshot_files`       COMMENT = '快照关联文件表';
ALTER TABLE `user`                 COMMENT = '用户表';

SELECT '表注释补全完成' AS '执行结果';

SELECT
    TABLE_NAME AS '表名',
    TABLE_COMMENT AS '注释'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'define_db'
  AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
