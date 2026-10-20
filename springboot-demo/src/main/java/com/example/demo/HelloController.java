package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 一个控制器，演示「动态网页」。
 *
 * 访问 http://localhost:8080/hello          -> 显示默认问候
 * 访问 http://localhost:8080/hello?name=张三 -> 显示带参数的问候
 *
 * 首页 http://localhost:8080/ 由 static/index.html 静态页面提供。
 */
@Controller
public class HelloController {

    @GetMapping(value = "/hello", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String hello(@RequestParam(value = "name", defaultValue = "Spring Boot") String name) {
        // 转义，防止把 HTML 标签当页面结构注入
        String safe = name.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;");

        return "<!DOCTYPE html>"
             + "<html lang=\"zh-CN\">"
             + "<head>"
             + "<meta charset=\"UTF-8\">"
             + "<title>Hello " + safe + "</title>"
             + "<style>"
             + "body{font-family:system-ui,'Microsoft YaHei',sans-serif;"
             + "background:#0f172a;color:#e2e8f0;display:flex;min-height:100vh;"
             + "align-items:center;justify-content:center;margin:0}"
             + ".card{background:#1e293b;padding:40px 56px;border-radius:14px;"
             + "box-shadow:0 12px 40px rgba(0,0,0,.45);text-align:center}"
             + "h1{margin:0 0 12px;color:#38bdf8}"
             + "p{color:#94a3b8;margin:8px 0}"
             + "a{color:#38bdf8;text-decoration:none}"
             + "a:hover{text-decoration:underline}"
             + "</style>"
             + "</head>"
             + "<body>"
             + "<div class=\"card\">"
             + "<h1>Hello, " + safe + "!</h1>"
             + "<p>这是 Spring Boot 动态生成的页面</p>"
             + "<p><a href=\"/\">&larr; 返回首页</a></p>"
             + "</div>"
             + "</body>"
             + "</html>";
    }
}
