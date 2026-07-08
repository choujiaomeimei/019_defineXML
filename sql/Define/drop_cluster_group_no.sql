-- 可选清理脚本：删除已废弃的 sas_cluster_group_no 表
-- 该表此前用于持久化候选组编号（group_no），现在编号体系已移除，此表不再被读写。
-- 执行前请确认已部署新版后端代码。

DROP TABLE IF EXISTS sas_cluster_group_no;
