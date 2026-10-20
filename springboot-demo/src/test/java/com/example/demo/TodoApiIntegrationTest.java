package com.example.demo;

import com.example.demo.model.Todo;
import com.example.demo.service.TodoService;
import com.example.demo.web.TodoController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【第四层】集成测试 —— 真实 HTTP、真实 Service、真实 Tomcat
 *
 * 和 TodoControllerTest（@WebMvcTest）的关键区别：
 *
 *   @WebMvcTest              |  @SpringBootTest(RANDOM_PORT)
 *   -------------------------|--------------------------------
 *   假的 HTTP 环境（MockMvc） | 真的启动 Tomcat，占用一个真实端口
 *   Service 是 @MockBean 假对象 | Service 是真的，数据真的存进内存
 *   只验证 Controller 逻辑   | 验证「整个链路」是否通畅
 *   快（约 1 秒）            | 慢（约 2-3 秒）
 *
 * 它最大的价值是能发现「单元测试永远发现不了」的问题：
 *   - Bean 装配对不对
 *   - JSON 序列化/反序列化有没有问题
 *   - 全局异常处理器生不生效
 *   - 静态资源能不能被访问到
 *   - 拦截器、过滤器、编码设置对不对
 *
 * RANDOM_PORT 的含义：随机分配一个空闲端口，避免和本机 8080 冲突，
 * 也让多个测试可以并行。用 @LocalServerPort 可以拿到实际端口号。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Todo API 集成测试（真实 HTTP）")
class TodoApiIntegrationTest {

    /** 会自动指向随机端口，相对路径（/api/todos）即可直接使用 */
    @Autowired
    private TestRestTemplate restTemplate;

    /** 注入真实的 Service，用来在测试开始前清理数据 */
    @Autowired
    private TodoService todoService;

    /** 随机端口号。TestRestTemplate 已经处理好了，这个字段主要用于演示 */
    @LocalServerPort
    private int port;

    /**
     * 重要：集成测试里的 Service 是「真身」，而且是整个测试类共用的单例。
     * 不清理的话，上一个测试方法留下的数据会污染下一个。
     */
    @BeforeEach
    void setUp() {
        todoService.clear();
    }

    // ==================== 完整生命周期 ====================

    @Test
    @DisplayName("完整生命周期：查询 -> 新增 -> 切换 -> 过滤 -> 删除")
    void fullLifecycle_overRealHttp() {
        // 1. 一开始应该是空的
        ResponseEntity<Todo[]> initial = restTemplate.getForEntity("/api/todos", Todo[].class);
        assertThat(initial.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(initial.getBody()).isEmpty();

        // 2. 新增两条
        Todo first = create("学习集成测试");
        Todo second = create("第二条数据");

        assertThat(first.getId()).isPositive();
        assertThat(first.getTitle()).isEqualTo("学习集成测试");
        assertThat(first.isDone()).isFalse();

        // 3. 列表应有 2 条，且保持插入顺序
        Todo[] all = restTemplate.getForEntity("/api/todos", Todo[].class).getBody();
        assertThat(all).hasSize(2);
        assertThat(all[0].getTitle()).isEqualTo("学习集成测试");
        assertThat(all[1].getTitle()).isEqualTo("第二条数据");

        // 4. 切换第一条为已完成
        Todo toggled = restTemplate
                .postForEntity("/api/todos/" + first.getId() + "/toggle", null, Todo.class)
                .getBody();
        assertThat(toggled.isDone()).isTrue();

        // 5. 只看未完成，应只剩第二条
        Todo[] unfinished = restTemplate
                .getForEntity("/api/todos?onlyUnfinished=true", Todo[].class)
                .getBody();
        assertThat(unfinished).hasSize(1);
        assertThat(unfinished[0].getTitle()).isEqualTo("第二条数据");

        // 6. 删除第二条 -> 应返回 204 No Content
        ResponseEntity<Void> deleted = restTemplate
                .exchange("/api/todos/" + second.getId(), HttpMethod.DELETE, null, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 7. 最终只剩 1 条
        assertThat(restTemplate.getForEntity("/api/todos", Todo[].class).getBody()).hasSize(1);
    }

    // ==================== 错误路径 ====================

    @Test
    @DisplayName("新增空标题 -> 400，且全局异常处理器返回了错误 JSON")
    void create_blankTitle_returns400() {
        TodoController.CreateRequest body = new TodoController.CreateRequest();
        body.setTitle("   ");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/todos", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // 验证 ApiExceptionHandler 真的生效了
        assertThat(response.getBody())
                .contains("BAD_REQUEST")
                .contains("待办内容不能为空");
    }

    @Test
    @DisplayName("查询不存在的编号 -> 404")
    void getOne_missing_returns404() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/todos/99999", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("切换不存在的编号 -> 404")
    void toggle_missing_returns404() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/todos/99999/toggle", null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("删除不存在的编号 -> 404")
    void delete_missing_returns404() {
        ResponseEntity<Void> response =
                restTemplate.exchange("/api/todos/99999", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================== 静态资源 ====================

    @Test
    @DisplayName("首页 / 能访问到，且是商城主页的 HTML")
    void indexPage_isServed() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("小确幸商城")
                .contains("product-grid");   // 页面里的商品列表容器
    }

    @Test
    @DisplayName("@LocalServerPort 能拿到随机端口，也可以用它拼完整地址")
    void localServerPort_isAvailable() {
        assertThat(port).isPositive();

        // 平时用相对路径就够了，这里演示一下端口确实可用
        ResponseEntity<String> response = restTemplate
                .getForEntity("http://localhost:" + port + "/api/todos", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ==================== 辅助方法 ====================

    /** 新增一条待办，并断言返回 201 */
    private Todo create(String title) {
        TodoController.CreateRequest body = new TodoController.CreateRequest();
        body.setTitle(title);

        ResponseEntity<Todo> response = restTemplate.postForEntity("/api/todos", body, Todo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
