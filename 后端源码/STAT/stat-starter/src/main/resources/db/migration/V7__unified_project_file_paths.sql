ALTER TABLE file_upload_records
    MODIFY COLUMN file_category ENUM(
        'P21_SPEC', 'XPT', 'PROJECT_SPEC', 'ACRF',
        'EDC_CODELIST', 'VLM', 'CODELIST'
    ) NOT NULL,
    ADD COLUMN workspace_file_path VARCHAR(500) NULL AFTER file_path,
    ADD COLUMN standard_type VARCHAR(20) NULL AFTER workspace_file_path;

ALTER TABLE file_version_history
    MODIFY COLUMN file_category ENUM(
        'P21_SPEC', 'XPT', 'PROJECT_SPEC', 'ACRF',
        'EDC_CODELIST', 'VLM', 'CODELIST'
    ) NOT NULL;

ALTER TABLE snapshot_files
    MODIFY COLUMN file_category ENUM(
        'P21_SPEC', 'XPT', 'PROJECT_SPEC', 'ACRF',
        'EDC_CODELIST', 'VLM', 'CODELIST'
    ) NOT NULL;

UPDATE file_upload_records r
LEFT JOIN project p ON p.project_id = r.project_id AND p.deleted = 0
SET r.standard_type = COALESCE(NULLIF(SUBSTRING_INDEX(p.standard_type, ',', 1), ''), 'SDTM')
WHERE r.standard_type IS NULL OR r.standard_type = '';
