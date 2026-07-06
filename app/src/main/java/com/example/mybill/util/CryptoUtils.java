package com.example.mybill.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class CryptoUtils {
    private CryptoUtils() {}

    /** 无盐SHA-256，用于兼容已有应用锁密码存储 */
    public static String sha256(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
