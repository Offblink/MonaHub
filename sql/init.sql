CREATE DATABASE IF NOT EXISTS submission_hub
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE submission_hub;
SET NAMES utf8mb4;

DROP TABLE IF EXISTS download_log;
DROP TABLE IF EXISTS file_info;
DROP TABLE IF EXISTS sys_user;

-- 用户表
CREATE TABLE sys_user (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50) NOT NULL                COMMENT '登录账号',
    password    VARCHAR(72) NOT NULL                COMMENT 'BCrypt 散列（$2a$10$...，自带随机盐）',
    salt        VARCHAR(32) NOT NULL DEFAULT ''     COMMENT '历史字段：BCrypt 自带盐，本列保留兼容任务书表结构',
    real_name   VARCHAR(50) DEFAULT NULL            COMMENT '显示名',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

-- 文件元信息
CREATE TABLE file_info (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键，下载只认这个 ID',
    origin_name    VARCHAR(255) NOT NULL                COMMENT '用户原始文件名，只存库',
    store_name     VARCHAR(64)  NOT NULL                COMMENT 'UUID+扩展名，只落盘',
    file_path      VARCHAR(512) NOT NULL                COMMENT '相对 upload.root 的路径',
    ext            VARCHAR(16)  NOT NULL                COMMENT '小写扩展名',
    md5            CHAR(32)     DEFAULT NULL            COMMENT '内容 MD5（重复上传去重）',
    file_size      BIGINT       NOT NULL                COMMENT '字节数',
    content_type   VARCHAR(128) DEFAULT NULL            COMMENT '上传时的 MIME',
    uploader_id    BIGINT       NOT NULL                COMMENT '上传人',
    create_time    DATETIME     NOT NULL                COMMENT '上传时间',
    status         TINYINT      NOT NULL DEFAULT 1      COMMENT '1 正常 / 0 已冻结',
    deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '0 正常 / 1 已逻辑删除（列表与下载都不可见）',
    download_count INT          NOT NULL DEFAULT 0      COMMENT '下载次数',
    PRIMARY KEY (id),
    KEY idx_uploader (uploader_id),
    KEY idx_uploader_time (uploader_id, create_time),
    KEY idx_uploader_md5 (uploader_id, md5),
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件元信息';

-- 下载日志（F5，课后拓展 / 本实现顺带完成）
CREATE TABLE download_log (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    file_id       BIGINT      NOT NULL COMMENT '被下载的文件',
    downloader_id BIGINT      NOT NULL COMMENT '下载人',
    download_time DATETIME    NOT NULL COMMENT '下载时间',
    ip            VARCHAR(64) DEFAULT NULL COMMENT '来源 IP',
    PRIMARY KEY (id),
    KEY idx_file (file_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '下载日志';

-- 演示账号（幂等：重复导入只覆盖同名账号的口令与显示名）
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