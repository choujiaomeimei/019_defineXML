-- ========================================
-- [已废弃] 修复 sas_cluster_group_no 表脏数据
-- 该表及编号体系已完全移除，请改用 drop_cluster_group_no.sql 删除该表。
-- ========================================
-- 用途: 清理"两个不同 cluster_key 共享同一 group_no"的脏数据
-- 背景: 旧版 executeMerge 会让新合并的 batch 继承源 cluster 的 group_no，
--       导致两个不同的候选组在前端都显示成同一个 #N（如截图中的两个 #5）。
--       修复后的 executeMerge 已不再继承，但需要清理已有脏数据。
-- 用法: 在数据库工具中执行；建议先运行"诊断"部分，确认问题再执行"清理"
-- ========================================

USE define_db;

-- ============= 第一步：诊断 =============

-- 1.1 查看是否存在重复的 group_no
SELECT 
    project_id, username, group_no, 
    COUNT(*) AS dup_count,
    GROUP_CONCAT(cluster_key SEPARATOR ' | ') AS cluster_keys
FROM sas_cluster_group_no
GROUP BY project_id, username, group_no
HAVING COUNT(*) > 1
ORDER BY project_id, username, group_no;

-- 1.2 查看可疑的"已撤销但残留"的 batch 记录
-- (cluster_key = bt:xxx 但 sas_codelist_merge_log 中已无该 batch_id)
SELECT g.project_id, g.username, g.cluster_key, g.group_no, g.created_time
FROM sas_cluster_group_no g
WHERE g.cluster_key LIKE 'bt:%'
  AND NOT EXISTS (
    SELECT 1 FROM sas_codelist_merge_log l
    WHERE l.project_id = g.project_id
      AND l.username = g.username
      AND CONCAT('bt:', l.merge_batch_id) = g.cluster_key
  );

-- ============= 第二步：清理（按需选择执行） =============

-- 2.1 清理重复 group_no：保留最早创建的一条，删除其余
-- 注意：这会让被删除的 cluster_key 在下次分析时获得新的 group_no
DELETE g1 FROM sas_cluster_group_no g1
INNER JOIN sas_cluster_group_no g2
WHERE g1.project_id = g2.project_id
  AND g1.username = g2.username
  AND g1.group_no = g2.group_no
  AND g1.id > g2.id;

-- 2.2 添加唯一性约束（防止以后再次出现重复）
-- 如果约束已存在，会报错，可以忽略
ALTER TABLE sas_cluster_group_no
    ADD UNIQUE KEY uk_pcgn (project_id, username, group_no);

-- ============= 第三步：验证 =============

-- 3.1 验证不再有重复
SELECT project_id, username, group_no, COUNT(*) AS dup_count
FROM sas_cluster_group_no
GROUP BY project_id, username, group_no
HAVING COUNT(*) > 1;
-- 期望: 0 行结果

-- 3.2 查看修复后的 group_no 分布
SELECT 
    project_id, username, 
    COUNT(*) AS total_clusters,
    MIN(group_no) AS min_no,
    MAX(group_no) AS max_no
FROM sas_cluster_group_no
GROUP BY project_id, username;
