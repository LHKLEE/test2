package com.example.demo.service;

/**
 * 文本处理工具。
 *
 * 为什么单独抽出来？因为「什么算空白」这件事原本在 TodoService、ProductService
 * 里各写了一遍，现在用户和购物车也要用，再复制就要出第四份了。
 *
 * 关键点（这是 TodoServiceEdgeCaseTest 抓出来的真实缺陷）：
 *   Java 8 的 {@code String.trim()} 只去除码点 &lt;= U+0020 的字符，
 *   而中文输入法常见的全角空格 {@code '\u3000'} 不在这个范围内。
 *   所以这里用 Character.isWhitespace（半角空白）+ Character.isSpaceChar（全角空格等）。
 *
 * 注：Java 11 起可以直接用 {@code String.isBlank()} / {@code strip()}，本项目是 Java 8。
 */
public final class Texts {

    private Texts() {
    }

    /** 是否为「空白」：null、空串，或全部由空白字符组成 */
    public static boolean isBlank(String text) {
        if (text == null) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!isBlankChar(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** 去掉首尾空白（含全角空格）；null 返回空串 */
    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        int start = 0;
        int end = text.length();
        while (start < end && isBlankChar(text.charAt(start))) {
            start++;
        }
        while (end > start && isBlankChar(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    /** 单个字符是否算空白 */
    private static boolean isBlankChar(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
}
