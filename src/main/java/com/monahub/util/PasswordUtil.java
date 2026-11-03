package com.monahub.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 口令散列：BCrypt（cost 10），哈希串形如 {@code $2a$10$...}。
 *
 * <p>BCrypt 自带随机盐并把盐写进哈希串，所以数据库不再需要单独的盐列；
 * 任务书表结构里的 {@code salt} 字段保留为兼容位（写空串），README 有说明。</p>
 *
 * <p>为什么换掉单轮 SHA-256：SHA-256 太快，泄露后可被 GPU 高速爆破；
 * BCrypt 按 cost 有意放慢，并对每个口令使用独立随机盐。</p>
 */
public final class PasswordUtil {

    /** cost=10 约 60–100ms/次，课内登录手感与安全性的折中。 */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    private PasswordUtil() {
    }

    /** 生成 BCrypt 哈希（含随机盐）。 */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /** 校验明文与库中哈希；空值一律失败，避免把 null 当匹配。 */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return ENCODER.matches(rawPassword, storedHash);
    }
}
