-- ============================================================
-- 增量迁移：重复上传去重所需的内容 MD5（2026-11-03）
-- 仅对已按旧版 01_schema.sql 建过库的环境执行一次；
-- 全新环境直接跑 01_schema.sql + 02_data.sql 即可。
--
-- 说明：历史记录的 md5 为 NULL，不参与去重（不影响使用）；
-- 从本次上线后的新上传开始，每条记录都会写入 MD5。
-- ============================================================
USE submission_hub;

ALTER TABLE file_info
    ADD COLUMN md5 CHAR(32) DEFAULT NULL
        COMMENT '内容 MD5（重复上传去重）' AFTER ext,
    ADD KEY idx_uploader_md5 (uploader_id, md5);
