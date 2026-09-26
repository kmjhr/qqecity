package com.icbc.qingqi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 演示用密码散列（SHA-256）。
 * 注意：正式化必须替换为加盐算法（如 BCrypt）并通过安全评审。
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    public static boolean matches(String raw, String hashed) {
        return hash(raw).equalsIgnoreCase(hashed);
    }
}
