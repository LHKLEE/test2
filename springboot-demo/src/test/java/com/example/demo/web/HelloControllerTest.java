package com.example.demo.web;

import com.example.demo.HelloController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HelloController 的测试。
 *
 * 为什么单独补这个类？
 *   之前的测试都集中在 Todo 相关功能上，/hello 这个接口其实一直「没人管」。
 *   这就是测试覆盖率的典型盲区：写测试时容易只盯着新功能，忘掉老接口。
 *
 * 另外这里演示了一个很重要的测试类型：**安全测试**。
 *   /hello 会把用户输入的 name 直接拼进 HTML。如果不做转义，
 *   别人传一段 <script> 就能在你页面上执行脚本（XSS 攻击）。
 *   安全相关的行为必须用测试锁死，否则以后有人「优化」代码时很容易改坏。
 */
@WebMvcTest(HelloController.class)
@DisplayName("HelloController（/hello 接口）")
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /hello 不传参数 -> 使用默认名字 Spring Boot")
    void hello_defaultName() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                // 断言返回的是 HTML，而不是 JSON 或纯文本
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Hello, Spring Boot!")));
    }

    @Test
    @DisplayName("GET /hello?name=张三 -> 名字出现在页面上（中文不能乱码）")
    void hello_customName() throws Exception {
        mockMvc.perform(get("/hello").param("name", "张三"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello, 张三!")));
    }

    @Test
    @DisplayName("安全：name 里的 <script> 必须被转义，不能原样输出")
    void hello_escapesScriptTag() throws Exception {
        mockMvc.perform(get("/hello").param("name", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                // 原始标签绝不能出现在响应里，否则就是 XSS 漏洞
                .andExpect(content().string(not(containsString("<script>"))))
                // 应该变成 HTML 实体
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    @Test
    @DisplayName("安全：& 也要转义，否则会破坏 HTML 结构")
    void hello_escapesAmpersand() throws Exception {
        mockMvc.perform(get("/hello").param("name", "A&B"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A&amp;B")));
    }
}
