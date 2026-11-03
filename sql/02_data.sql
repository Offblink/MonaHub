-- ============================================================
-- MonaHub 演示账号（可重复执行，幂等）
--   zhangsan / 123456   张三
--   lisi     / 123456   李四
-- password = sha256(salt + ':' + 明文) 的十六进制；库里没有任何明文。
-- ============================================================
USE submission_hub;

INSERT INTO sys_user (username, password, salt, real_name, create_time)
VALUES ('zhangsan',
        '273c5af299806d5610e7b0a517395f2176a08f056447489349e4e70af7028d01',
        'a1b2c3d4e5f60718293a4b5c6d7e8f90',
        '张三',
        NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), salt = VALUES(salt), real_name = VALUES(real_name);

INSERT INTO sys_user (username, password, salt, real_name, create_time)
VALUES ('lisi',
        'a44a4f668b0fdf0fcf2fd615ba196bfd0888e5fa3cb8477e891386dd8dee8316',
        '0f1e2d3c4b5a69788796a5b4c3d2e1f0',
        '李四',
        NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), salt = VALUES(salt), real_name = VALUES(real_name);
