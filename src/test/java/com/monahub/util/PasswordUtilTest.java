package com.monahub.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 口令加盐散列：同口令不同盐 → 不同密文；校验严格。
 */
class PasswordUtilTest {

    @Test
    void hashIsDeterministicForSameSalt() {
        String salt = PasswordUtil.newSalt();
        assertEquals(PasswordUtil.hash("123456", salt), PasswordUtil.hash("123456", salt));
        assertEquals(32, salt.length());
        assertEquals(64, PasswordUtil.hash("123456", salt).length());
    }

    @Test
    void samePasswordWithDifferentSaltsProducesDifferentHashes() {
        String hashA = PasswordUtil.hash("123456", PasswordUtil.newSalt());
        String hashB = PasswordUtil.hash("123456", PasswordUtil.newSalt());
        assertNotEquals(hashA, hashB);
    }

    @Test
    void verifyAcceptsCorrectPasswordAndRejectsOthers() {
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash("correct horse", salt);
        assertTrue(PasswordUtil.verify("correct horse", salt, hash));
        assertFalse(PasswordUtil.verify("correct hors", salt, hash));
        assertFalse(PasswordUtil.verify("", salt, hash));
        assertFalse(PasswordUtil.verify("correct horse", salt, null));
        assertFalse(PasswordUtil.verify(null, salt, hash));
    }

    @Test
    void hashIsNotPlaintext() {
        String salt = PasswordUtil.newSalt();
        assertNotEquals("123456", PasswordUtil.hash("123456", salt));
    }
}
