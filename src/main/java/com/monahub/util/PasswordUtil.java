package com.monahub.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * 口令加盐散列：salt 为 16 字节随机数（十六进制 32 位），
 * password = SHA-256(salt + ":" + 明文) 的十六进制。
 *
 * <p>为什么不选 AES：AES 是可逆的，一旦库泄露等于泄露明文；
 * 口令只需要"能验证、不能还原"，必须用单向散列。</p>
 *
 * <p>说明：课内演示用单轮 SHA-256 足够说明"加盐 + 不明文"；
 * 生产环境应换成 PBKDF2/BCrypt 并提高迭代次数（README 拓展项已注明）。</p>
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasswordUtil() {
    }

    /** 生成 16 字节随机盐的十六进制串。 */
    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    /** 计算 SHA-256(salt + ":" + rawPassword) 的十六进制。 */
    public static String hash(String rawPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            return toHex(digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // JDK 必须提供 SHA-256，走到这里说明运行环境损坏
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    /** 常量时间比较，避免按字节短路带来的旁路信息。 */
    public static boolean verify(String rawPassword, String salt, String expectedHash) {
        if (rawPassword == null || salt == null || expectedHash == null) {
            return false;
        }
        String actual = hash(rawPassword, salt);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
