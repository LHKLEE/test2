package com.example.demo.web;

import com.example.demo.model.Todo;
import com.example.demo.service.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 待办事项的 REST 接口 —— 用来学习「Web 层怎么测」。
 *
 * 接口清单：
 *   GET    /api/todos                 查询全部（?onlyUnfinished=true 只看未完成）
 *   GET    /api/todos/{id}            查询单条
 *   POST   /api/todos                 新增，body: {"title":"..."}
 *   POST   /api/todos/{id}/toggle     切换完成状态
 *   DELETE /api/todos/{id}            删除
 *
 * 测试这一类代码通常用 MockMvc：不启动真实端口，直接模拟 HTTP 请求，
 * 然后断言「状态码」「响应 JSON」是否符合预期。
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    /** 构造器注入：测试时可以用 @MockBean 换成假的 TodoService */
    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /** 查询列表 */
    @GetMapping
    public List<Todo> list(
            @RequestParam(value = "onlyUnfinished", defaultValue = "false") boolean onlyUnfinished) {
        return onlyUnfinished ? todoService.findUnfinished() : todoService.findAll();
    }

    /** 查询单条。不存在时返回 404 */
    @GetMapping("/{id}")
    public ResponseEntity<Todo> getOne(@PathVariable("id") long id) {
        Optional<Todo> found = todoService.findById(id);
        if (found.isPresent()) {
            return ResponseEntity.ok(found.get());
        }
        return ResponseEntity.notFound().build();
    }

    /** 新增。成功返回 201；内容为空由 ApiExceptionHandler 转成 400 */
    @PostMapping
    public ResponseEntity<Todo> create(@RequestBody CreateRequest request) {
        Todo created = todoService.add(request == null ? null : request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** 切换完成状态。不存在时返回 404 */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<Todo> toggle(@PathVariable("id") long id) {
        Optional<Todo> toggled = todoService.toggle(id);
        if (toggled.isPresent()) {
            return ResponseEntity.ok(toggled.get());
        }
        return ResponseEntity.notFound().build();
    }

    /** 删除。成功返回 204；不存在返回 404 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) {
        if (todoService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 新增请求的 body 结构，对应 JSON：{"title":"写第一个测试"}
     *
     * 单独定义一个类，比直接用 Map 更清晰，也方便以后加校验注解。
     */
    public static class CreateRequest {

        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
