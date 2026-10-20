package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 密码工具：加盐 + SHA-256。
 *
 * 为什么不直接存明文？因为一旦数据库泄露，明文密码会连累用户在其他网站的账号。
 * 加盐的作用是：即使两个用户用同一个密码，存进数据库的散列值也完全不同，
 * 攻击者没法用一张「常见密码彩虹表」一次性撞出所有账号。
 *
 * 存进 t_user 的是两列：
 *   salt          —— 每个用户独立的随机盐（32 位十六进制）
 *   password_hash —— sha256(salt + ":" + 明文密码) 的十六进制
 * 校验时用同样的算法重算一遍再比对，数据库里永远没有明文。
 *
 * ⚠️ 这是教学用的简化实现。生产环境请直接用
 * Spring Security 的 BCryptPasswordEncoder 或 Argon2 —— 它们比 SHA-256 慢得多，
 * 而且故意设计成「慢」，专门用来抵御暴力破解。
 */
public final class Passwords {

    /** 加密安全的随机数发生器，不要用 java.util.Random */
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** 工具类不允许被实例化 */
    private Passwords() {
    }

    /** 生成 16 字节随机盐，返回 32 位十六进制字符串 */
    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    /**
     * 计算散列值：sha256( salt + ":" + password )
     *
     * @param password 明文密码，调用方保证非 null
     * @param salt     该用户的盐
     * @return 64 位十六进制字符串
     */
    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // 标准 JDK 一定支持 SHA-256，正常走不到这里
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    /**
     * 校验明文密码是否与库里存的散列值匹配。
     *
     * 用 MessageDigest.isEqual 而不是 String.equals：前者是常量时间比较，
     * 不会因为「第几个字符开始不一样」而提前返回，从而泄露信息。
     */
    public static boolean matches(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        byte[] actual = hash(password, salt).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }

    /** 字节数组转小写十六进制 */
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
