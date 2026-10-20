package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 【第零层】冒烟测试（Smoke Test）—— 最简单也最重要的一条
 *
 * @SpringBootTest 会启动「完整的 Spring 容器」，把所有的 @Service、@Controller、
 * @RestControllerAdvice 都真实装配起来。
 *
 * 它验证的是：所有 Bean 能不能装配成功、配置有没有写错、依赖有没有缺。
 * 很多低级错误（比如构造器注入的参数对不上、Bean 循环依赖）都能被这一条测出来。
 *
 * 代价是比 @WebMvcTest 慢（要启动整个容器），所以别在里面塞太多业务断言，
 * 具体逻辑交给下面两层去测。
 *
 * 注意：这里的 webEnvironment 默认是 MOCK，也就是「不真的占用 8080 端口」。
 * 如果想用真实端口做端到端测试，可以写：
 *     @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * 然后配合 @Autowired TestRestTemplate 发真实 HTTP 请求。
 */
@SpringBootTest
class DemoApplicationTests {

    @Test
    void contextLoads() {
        // 方法体是空的：只要容器能启动起来，测试就算通过。
        // 如果启动失败，这里会直接抛异常并打印完整的失败原因。
    }
}
