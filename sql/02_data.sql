-- ============================================================
-- MonaHub 演示账号（可重复执行，幂等）
--   zhangsan / 123456   张三
--   lisi     / 123456   李四
--
-- 口令算法：BCrypt（cost 10，$2a$ 前缀，哈希内自带随机盐）。
-- salt 列是任务书表结构的历史字段，BCrypt 不再单独使用，统一写空串。
-- 库里没有任何明文口令。
-- 如需改口令：用 BCrypt 生成 "$2a$10$..." 后替换下面的 password 值。
-- ============================================================
USE submission_hub;

INSERT INTO sys_user (username, password, salt, real_name, create_time)
VALUES ('zhangsan',
        '$2a$10$Wcip6Fc8KzNwbFYGp./Ogu3L2KL.xiXr6RLVecyhtSu88pyJywQOe',
        '',
        '张三',
        NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), salt = VALUES(salt), real_name = VALUES(real_name);

INSERT INTO sys_user (username, password, salt, real_name, create_time)
VALUES ('lisi',
        '$2a$10$OxnOosdGyXwk5RrrVqf2Cum0KszobueRqppG.Dnc8/vX6qVcVZesC',
        '',
        '李四',
        NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), salt = VALUES(salt), real_name = VALUES(real_name);
