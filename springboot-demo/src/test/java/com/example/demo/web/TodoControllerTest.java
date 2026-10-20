package com.example.demo.web;

import com.example.demo.model.Todo;
import com.example.demo.service.TodoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【第三层】Web 层测试
 *
 * 核心区别：这一层测的是「HTTP 接口」，所以关心的不再是 Java 方法返回值，
 * 而是「状态码对不对」「返回的 JSON 长什么样」。
 *
 * 两个关键注解：
 *
 * @WebMvcTest(TodoController.class)
 *     只启动 Web 层（Controller + 参数解析 + JSON 转换 + 异常处理），
 *     不启动整个 Spring 容器，所以比 @SpringBootTest 快很多。
 *     注意它不会加载 @Service，所以下面的 TodoService 必须自己提供。
 *
 * @MockBean
 *     把一个「假的」TodoService 放进容器。它是 Mockito 造的替身对象，
 *     所有方法的默认返回值都是 null/false/0，需要什么行为就用 given(...) 提前约定。
 *
 *     为什么要把 Service 换成假的？
 *     因为这一层我们只想验证「Controller 处理 HTTP 的逻辑」，
 *     不想被 Service 的真实业务逻辑干扰。这样测试更纯粹、更快，也更容易造异常场景。
 */
@WebMvcTest(TodoController.class)
@DisplayName("TodoController（MockMvc + Mockito）")
class TodoControllerTest {

    /** Spring 提供的模拟 HTTP 客户端，不用真的启动 8080 端口 */
    @Autowired
    private MockMvc mockMvc;

    /** 被替换成替身的 Service */
    @MockBean
    private TodoService todoService;

    // ==================== GET /api/todos ====================

    @Test
    @DisplayName("GET /api/todos -> 200，返回 JSON 数组")
    void list_shouldReturnJsonArray() throws Exception {
        // given：约定假 Service 被调用时返回什么
        given(todoService.findAll()).willReturn(Arrays.asList(
                new Todo(1L, "写测试", false),
                new Todo(2L, "跑测试", true)
        ));

        // when + then：发请求并断言响应
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())                  // 断言状态码 200
                .andExpect(jsonPath("$", hasSize(2)))        // 断言数组长度
                .andExpect(jsonPath("$[0].id").value(1))     // 断言第一个元素
                .andExpect(jsonPath("$[0].title").value("写测试"))
                .andExpect(jsonPath("$[0].done").value(false))
                .andExpect(jsonPath("$[1].done").value(true));
    }

    @Test
    @DisplayName("GET /api/todos -> 没有数据时返回空数组 []")
    void list_empty_shouldReturnEmptyArray() throws Exception {
        given(todoService.findAll()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/todos?onlyUnfinished=true -> 调用的是 findUnfinished")
    void list_onlyUnfinished_shouldDelegate() throws Exception {
        given(todoService.findUnfinished())
                .willReturn(Collections.singletonList(new Todo(1L, "未完成", false)));

        mockMvc.perform(get("/api/todos").param("onlyUnfinished", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // 验证「确实调用了 findUnfinished」而不是 findAll
        then(todoService).should().findUnfinished();
    }

    // ==================== GET /api/todos/{id} ====================

    @Test
    @DisplayName("GET /api/todos/1 -> 存在时 200")
    void getOne_found_shouldReturn200() throws Exception {
        given(todoService.findById(1L)).willReturn(Optional.of(new Todo(1L, "写测试", false)));

        mockMvc.perform(get("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("写测试"));
    }

    @Test
    @DisplayName("GET /api/todos/99 -> 不存在时 404")
    void getOne_missing_shouldReturn404() throws Exception {
        given(todoService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/todos/99"))
                .andExpect(status().isNotFound());
    }

    // ==================== POST /api/todos ====================

    @Test
    @DisplayName("POST /api/todos -> 201 Created，响应体是新对象")
    void create_shouldReturn201() throws Exception {
        given(todoService.add("新任务")).willReturn(new Todo(1L, "新任务", false));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)          // 必须声明是 JSON
                        .content("{\"title\":\"新任务\"}"))                // 请求体
                .andExpect(status().isCreated())                          // 201
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("新任务"))
                .andExpect(jsonPath("$.done").value(false));

        // 验证参数确实透传给了 Service
        then(todoService).should().add("新任务");
    }

    @Test
    @DisplayName("POST /api/todos 标题为空 -> 400，且走的是全局异常处理")
    void create_blankTitle_shouldReturn400() throws Exception {
        // 让假 Service 抛异常，模拟真实 Service 的校验行为
        given(todoService.add("")).willThrow(new IllegalArgumentException("待办内容不能为空"));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())                       // 400
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))       // ApiExceptionHandler 的输出格式
                .andExpect(jsonPath("$.message").value("待办内容不能为空"));
    }

    // ==================== POST /api/todos/{id}/toggle ====================

    @Test
    @DisplayName("POST /api/todos/1/toggle -> 200，done 变为 true")
    void toggle_found() throws Exception {
        given(todoService.toggle(1L)).willReturn(Optional.of(new Todo(1L, "写测试", true)));

        mockMvc.perform(post("/api/todos/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));
    }

    @Test
    @DisplayName("POST /api/todos/99/toggle -> 404")
    void toggle_missing() throws Exception {
        given(todoService.toggle(99L)).willReturn(Optional.empty());

        mockMvc.perform(post("/api/todos/99/toggle"))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE /api/todos/{id} ====================

    @Test
    @DisplayName("DELETE /api/todos/1 -> 204 No Content")
    void delete_found() throws Exception {
        given(todoService.delete(1L)).willReturn(true);

        mockMvc.perform(delete("/api/todos/1"))
                .andExpect(status().isNoContent());      // 204 表示成功但没有响应体
    }

    @Test
    @DisplayName("DELETE /api/todos/99 -> 404")
    void delete_missing() throws Exception {
        given(todoService.delete(99L)).willReturn(false);

        mockMvc.perform(delete("/api/todos/99"))
                .andExpect(status().isNotFound());
    }
}
