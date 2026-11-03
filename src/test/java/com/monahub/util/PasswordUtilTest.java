package com.monahub.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BCrypt 口令散列：哈希自带随机盐、同一口令两次结果不同、校验严格。
 */
class PasswordUtilTest {

    @Test
    void encodeProducesBcryptHashNotPlaintext() {
        String hash = PasswordUtil.encode("123456");
        assertTrue(hash.startsWith("$2a$10$"), hash);
        assertNotEquals("123456", hash);
        assertTrue(hash.length() >= 60, hash);
    }

    @Test
    void samePasswordGetsDifferentHashesBecauseOfRandomSalt() {
        assertNotEquals(PasswordUtil.encode("123456"), PasswordUtil.encode("123456"));
    }

    @Test
    void matchesAcceptsCorrectPasswordAndRejectsOthers() {
        String hash = PasswordUtil.encode("correct horse");
        assertTrue(PasswordUtil.matches("correct horse", hash));
        assertFalse(PasswordUtil.matches("correct hors", hash));
        assertFalse(PasswordUtil.matches("", hash));
        assertFalse(PasswordUtil.matches(null, hash));
        assertFalse(PasswordUtil.matches("correct horse", null));
        assertFalse(PasswordUtil.matches("correct horse", ""));
    }
}
