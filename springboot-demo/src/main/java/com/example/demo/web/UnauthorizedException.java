package com.example.demo.web;

/**
 * 未登录 / 登录态失效。
 *
 * 单独定义一个异常类型，是为了让 ApiExceptionHandler 能把它翻译成
 * HTTP 401（Unauthorized）而不是笼统的 400。
 *
 * 前端就是靠这个区分两种情况的：
 *   400 —— 你填的内容有问题（比如用户名已被注册）
 *   401 —— 你还没登录（这时应该弹出登录框）
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
