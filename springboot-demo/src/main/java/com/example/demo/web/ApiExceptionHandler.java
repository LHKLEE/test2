package com.example.demo.web;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理 —— 把业务异常统一转成规范的 HTTP 响应。
 *
 * @RestControllerAdvice 会拦截所有 Controller 抛出的异常。
 * 这里处理四类：
 *   IllegalArgumentException       -> 400  内容有问题（标题为空、用户名重复、验证码错……）
 *   UnauthorizedException          -> 401  没登录 / 登录态失效
 *   MaxUploadSizeExceededException -> 400  上传的文件太大
 *   RedisConnectionFailureException-> 503  验证码依赖的 Redis 连不上
 *
 * 这样 Controller 里就不用写 try-catch 了，代码更干净；
 * 前端也能靠状态码区分「你填错了」和「你还没登录」。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return body(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage());
    }

    /**
     * 上传文件超过 spring.servlet.multipart.max-file-size 时，Spring 会在
     * 请求进入 Controller 之前就抛出这个异常。
     *
     * 不处理的话会落到 Spring 默认的 500 "Internal Server Error"，
     * 用户传张大图只看到一句英文报错，根本不知道是尺寸问题 —— 属于典型的
     * 「技术上是 5xx，业务上其实是用户输入问题」，应该按 400 返回并说清原因。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "图片太大了，头像不能超过 2 MB");
    }

    /**
     * Redis 连不上（没启动、端口不通、密码错……）。
     *
     * 为什么单独处理？
     *   验证码依赖 Redis。Redis 挂了的话，用户点登录只会看到一句 500，
     *   完全猜不到是"验证码服务不可用"。用 503（Service Unavailable）
     *   既符合语义（是依赖服务不可用，不是请求有问题），也让前端能给出
     *   「验证码服务暂不可用，请稍后再试」这种能看懂的话。
     *
     * 顺带一个排查提示：本机 Redis 是 Windows 服务（名为 Redis），
     *   Win+R 输入 services.msc 就能看到它是不是停了。
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<Map<String, Object>> handleRedisDown(RedisConnectionFailureException ex) {
        return body(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA_UNAVAILABLE",
                "验证码服务暂不可用（Redis 未连接），请稍后再试");
    }

    /** 几种异常的输出格式保持一致，前端可以只写一套解析逻辑 */
    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
