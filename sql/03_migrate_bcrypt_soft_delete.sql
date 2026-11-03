-- ============================================================
-- 增量迁移：BCrypt 口令 + 逻辑删除 + 重命名（2026-11-03）
-- 仅对"已经按旧版 01_schema.sql 建过库"的环境执行一次；
-- 全新环境直接跑 01_schema.sql + 02_data.sql 即可，不必执行本文件。
-- ============================================================
USE submission_hub;

-- 1) 逻辑删除列（列表/下载默认过滤 deleted = 0）
ALTER TABLE file_info
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0
        COMMENT '0 正常 / 1 已逻辑删除' AFTER status;

-- 2) BCrypt 散列最长 60 位，password 放宽到 72；salt 改为兼容字段
ALTER TABLE sys_user
    MODIFY COLUMN password VARCHAR(72) NOT NULL
        COMMENT 'BCrypt 散列（$2a$10$...，自带随机盐）',
    MODIFY COLUMN salt VARCHAR(32) NOT NULL DEFAULT ''
        COMMENT '历史字段：BCrypt 自带盐，本列保留兼容';

-- 3) 重置演示账号口令为 123456 的 BCrypt 散列
UPDATE sys_user
SET password = '$2a$10$Wcip6Fc8KzNwbFYGp./Ogu3L2KL.xiXr6RLVecyhtSu88pyJywQOe',
    salt = ''
WHERE username = 'zhangsan';

UPDATE sys_user
SET password = '$2a$10$OxnOosdGyXwk5RrrVqf2Cum0KszobueRqppG.Dnc8/vX6qVcVZesC',
    salt = ''
WHERE username = 'lisi';
