-- ========================================
-- 项目数据完整重置脚本
-- 清空所有业务数据，保留表结构和默认项目
-- 更新时间: 2026-04-10
-- 所有表均做 IF EXISTS 安全检查，未建的表自动跳过
-- 覆盖表(18张):
--   user, project, project_config, project_member,
--   sas_project_spec, sas_vlm_data, sas_codelist_data,
--   sas_methods_data, sas_comments_data,
--   sas_pages_data, sas_datasets_data,
--   define_sheet_data,
--   sdrg_content, sdrg_template,
--   file_upload_records, file_version_history,
--   project_snapshot, snapshot_files
-- ========================================
USE define_db;

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

DROP PROCEDURE IF EXISTS `_do_reset_project`;
DELIMITER //
CREATE PROCEDURE `_do_reset_project`()
BEGIN
    DECLARE _db VARCHAR(50) DEFAULT 'define_db';

    -- ========================================
    -- 1. 版本管理相关表
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'snapshot_files') THEN
        DELETE FROM `snapshot_files` WHERE 1=1;
        ALTER TABLE `snapshot_files` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project_snapshot') THEN
        DELETE FROM `project_snapshot` WHERE 1=1;
        ALTER TABLE `project_snapshot` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'file_version_history') THEN
        DELETE FROM `file_version_history` WHERE 1=1;
        ALTER TABLE `file_version_history` AUTO_INCREMENT = 1;
    END IF;

    -- ========================================
    -- 2. 文件上传记录
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'file_upload_records') THEN
        DELETE FROM `file_upload_records` WHERE 1=1;
        ALTER TABLE `file_upload_records` AUTO_INCREMENT = 1;
    END IF;

    -- ========================================
    -- 3. 核心业务数据表
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_project_spec') THEN
        DELETE FROM `sas_project_spec` WHERE 1=1;
        ALTER TABLE `sas_project_spec` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_vlm_data') THEN
        DELETE FROM `sas_vlm_data` WHERE 1=1;
        ALTER TABLE `sas_vlm_data` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_codelist_data') THEN
        DELETE FROM `sas_codelist_data` WHERE 1=1;
        ALTER TABLE `sas_codelist_data` AUTO_INCREMENT = 1;
    END IF;

    -- ========================================
    -- 4. Methods / Comments / Pages / Datasets
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_methods_data') THEN
        DELETE FROM `sas_methods_data` WHERE 1=1;
        ALTER TABLE `sas_methods_data` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_comments_data') THEN
        DELETE FROM `sas_comments_data` WHERE 1=1;
        ALTER TABLE `sas_comments_data` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_pages_data') THEN
        DELETE FROM `sas_pages_data` WHERE 1=1;
        ALTER TABLE `sas_pages_data` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_datasets_data') THEN
        DELETE FROM `sas_datasets_data` WHERE 1=1;
        ALTER TABLE `sas_datasets_data` AUTO_INCREMENT = 1;
    END IF;

    -- ========================================
    -- 5. Define Sheet 数据表
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'define_sheet_data') THEN
        DELETE FROM `define_sheet_data` WHERE 1=1;
        ALTER TABLE `define_sheet_data` AUTO_INCREMENT = 1;
    END IF;

    -- ========================================
    -- 6. SDRG 相关表
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sdrg_content') THEN
        DELETE FROM `sdrg_content` WHERE 1=1;
        ALTER TABLE `sdrg_content` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sdrg_template') THEN
        DELETE FROM `sdrg_template` WHERE 1=1;
        ALTER TABLE `sdrg_template` AUTO_INCREMENT = 1;
    END IF;

    -- ========================================
    -- 7. 项目成员表
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project_member') THEN
        DELETE FROM `project_member` WHERE 1=1;
        ALTER TABLE `project_member` AUTO_INCREMENT = 1;
    END IF;

    -- ========================================
    -- 8. 项目配置
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project_config') THEN
        DELETE FROM `project_config` WHERE 1=1;
        ALTER TABLE `project_config` AUTO_INCREMENT = 1;
    END IF;

    -- ========================================
    -- 9. 用户表 → 重建默认管理员
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'user') THEN
        DELETE FROM `user` WHERE 1=1;
        ALTER TABLE `user` AUTO_INCREMENT = 1;
        INSERT INTO `user` (`username`, `password`, `email`)
        VALUES ('admin', '$2a$10$defaultHashedPasswordPlaceholder', 'admin@system.local');
    END IF;

    -- ========================================
    -- 10. 项目表 → 重建默认项目
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project') THEN
        DELETE FROM `project` WHERE 1=1;
        ALTER TABLE `project` AUTO_INCREMENT = 1;
        INSERT INTO `project` (`project_id`, `project_name`, `username`, `status`, `standard_type`, `created_time`, `updated_time`, `deleted`)
        VALUES
          ('DEFAULT', '默认项目', 'admin', 1, 'SDTM', NOW(), NOW(), 0),
          ('P000_Demo', '模板项目', 'admin', 1, 'SDTM,ADAM', NOW(), NOW(), 0);
    END IF;

    -- ========================================
    -- 11. 重建默认项目配置
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project_config') THEN
        INSERT INTO `project_config` (`project_id`, `encoding`, `language`, `standard_type`, `standard_version`, `ct_version`, `chinese_standard`, `english_standard`, `source_format`, `configuration`, `creator`)
        VALUES
          ('DEFAULT', 'UTF-8', 'CN', 'SDTM', 'SDTM-IG3.2', 'CT2021-12-17', TRUE, FALSE, 'SAS(XPT)', 'SDTM-IG3.2(NMPA)', 'admin'),
          ('P000_Demo', 'UTF-8', 'CN', 'SDTM', 'SDTM-IG3.2', 'CT2021-12-17', TRUE, FALSE, 'SAS(XPT)', 'SDTM-IG3.2(NMPA)', 'admin');
    END IF;

    -- ========================================
    -- 12. 重建默认项目成员
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project_member') THEN
        INSERT INTO `project_member` (`project_id`, `username`, `role`)
        VALUES
          ('DEFAULT', 'admin', 'owner'),
          ('P000_Demo', 'admin', 'owner');
    END IF;

    -- ========================================
    -- 13. 清空旧的分类上传表（如果存在）
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_acrf_upload') THEN
        DELETE FROM `sas_acrf_upload` WHERE 1=1;
        ALTER TABLE `sas_acrf_upload` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_p21_spec_upload') THEN
        DELETE FROM `sas_p21_spec_upload` WHERE 1=1;
        ALTER TABLE `sas_p21_spec_upload` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_project_spec_upload') THEN
        DELETE FROM `sas_project_spec_upload` WHERE 1=1;
        ALTER TABLE `sas_project_spec_upload` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_xpt_upload') THEN
        DELETE FROM `sas_xpt_upload` WHERE 1=1;
        ALTER TABLE `sas_xpt_upload` AUTO_INCREMENT = 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'p21_generation_task') THEN
        DELETE FROM `p21_generation_task` WHERE 1=1;
        ALTER TABLE `p21_generation_task` AUTO_INCREMENT = 1;
    END IF;

END //
DELIMITER ;

CALL `_do_reset_project`();
DROP PROCEDURE IF EXISTS `_do_reset_project`;

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- ========================================
-- 验证结果
-- ========================================
DROP PROCEDURE IF EXISTS `_reset_verify`;
DELIMITER //
CREATE PROCEDURE `_reset_verify`()
BEGIN
    DECLARE _db VARCHAR(50) DEFAULT 'define_db';

    SELECT '===== 项目重置完成 =====' AS '';

    -- 核心表
    SELECT '项目表' AS '表名', COUNT(*) AS '记录数' FROM project
    UNION ALL SELECT '项目配置', COUNT(*) FROM project_config
    UNION ALL SELECT 'Spec数据', COUNT(*) FROM sas_project_spec
    UNION ALL SELECT 'VLM数据', COUNT(*) FROM sas_vlm_data
    UNION ALL SELECT 'CodeList', COUNT(*) FROM sas_codelist_data
    UNION ALL SELECT '文件上传记录', COUNT(*) FROM file_upload_records;

    -- 可选表逐个安全查询
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'user') THEN
        SELECT '用户' AS '表名', COUNT(*) AS '记录数' FROM `user`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project_member') THEN
        SELECT '项目成员' AS '表名', COUNT(*) AS '记录数' FROM `project_member`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_methods_data') THEN
        SELECT 'Methods' AS '表名', COUNT(*) AS '记录数' FROM `sas_methods_data`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_comments_data') THEN
        SELECT 'Comments' AS '表名', COUNT(*) AS '记录数' FROM `sas_comments_data`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_pages_data') THEN
        SELECT 'Pages数据' AS '表名', COUNT(*) AS '记录数' FROM `sas_pages_data`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sas_datasets_data') THEN
        SELECT 'Datasets' AS '表名', COUNT(*) AS '记录数' FROM `sas_datasets_data`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'define_sheet_data') THEN
        SELECT 'DefineSheet' AS '表名', COUNT(*) AS '记录数' FROM `define_sheet_data`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sdrg_content') THEN
        SELECT 'SDRG内容' AS '表名', COUNT(*) AS '记录数' FROM `sdrg_content`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'sdrg_template') THEN
        SELECT 'SDRG模板' AS '表名', COUNT(*) AS '记录数' FROM `sdrg_template`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'file_version_history') THEN
        SELECT '文件历史版本' AS '表名', COUNT(*) AS '记录数' FROM `file_version_history`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project_snapshot') THEN
        SELECT '项目快照' AS '表名', COUNT(*) AS '记录数' FROM `project_snapshot`;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'snapshot_files') THEN
        SELECT '快照文件' AS '表名', COUNT(*) AS '记录数' FROM `snapshot_files`;
    END IF;

    SELECT '默认项目:' AS '';
    SELECT project_id, project_name, username, standard_type FROM project;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'user') THEN
        SELECT '默认用户:' AS '';
        SELECT username, email FROM `user`;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = _db AND table_name = 'project_member') THEN
        SELECT '项目成员:' AS '';
        SELECT project_id, username, role FROM project_member;
    END IF;
END //
DELIMITER ;

CALL `_reset_verify`();
DROP PROCEDURE IF EXISTS `_reset_verify`;
