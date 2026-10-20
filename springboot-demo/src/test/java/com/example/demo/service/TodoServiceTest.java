package com.example.demo.service;

import com.example.demo.model.Todo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【第二层】带状态的业务逻辑测试
 *
 * 和第一层的区别：TodoService 内部有数据（一个 Map），
 * 所以必须保证「每个测试方法都从空白状态开始」——这就是 @BeforeEach + clear() 的作用。
 *
 * 如果不清理会怎样？
 *   测试 A 新增了一条，测试 B 断言 count()==1 时就会看到 2，出现「单独跑通过、
 *   一起跑就失败」的诡异现象。这是新手最常踩的坑。
 */
@DisplayName("TodoService（业务逻辑测试）")
class TodoServiceTest {

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService();
        todoService.clear();   // 双保险：确保每次都是全新的服务
    }

    // ==================== add ====================

    @Test
    @DisplayName("新增后应能查到，且初始为未完成")
    void add_shouldCreateTodo() {
        Todo created = todoService.add("写第一个测试");

        assertNotNull(created);
        assertEquals(1L, created.getId());
        assertEquals("写第一个测试", created.getTitle());
        assertFalse(created.isDone(), "新建的待办应该是未完成状态");
        assertEquals(1, todoService.count());
    }

    @Test
    @DisplayName("编号应自增：1、2、3")
    void add_shouldIncrementId() {
        assertEquals(1L, todoService.add("A").getId());
        assertEquals(2L, todoService.add("B").getId());
        assertEquals(3L, todoService.add("C").getId());
    }

    @Test
    @DisplayName("标题首尾空格应被去掉")
    void add_shouldTrimTitle() {
        Todo created = todoService.add("   写测试   ");
        assertEquals("写测试", created.getTitle());
    }

    @Test
    @DisplayName("标题为空/null/纯空格时应抛异常")
    void add_blankTitle_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> todoService.add(""));
        assertThrows(IllegalArgumentException.class, () -> todoService.add("   "));
        assertThrows(IllegalArgumentException.class, () -> todoService.add(null));
    }

    // ==================== findAll ====================

    @Test
    @DisplayName("findAll 应按插入顺序返回")
    void findAll_shouldKeepInsertionOrder() {
        todoService.add("第一");
        todoService.add("第二");
        todoService.add("第三");

        List<Todo> all = todoService.findAll();
        assertEquals(3, all.size());
        assertEquals("第一", all.get(0).getTitle());
        assertEquals("第二", all.get(1).getTitle());
        assertEquals("第三", all.get(2).getTitle());
    }

    @Test
    @DisplayName("findAll 没有数据时返回空列表（不是 null）")
    void findAll_emptyReturnsEmptyList() {
        List<Todo> all = todoService.findAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    /**
     * 这条测的是「封装」：外部拿到列表后乱改，不该影响服务内部的数据。
     * 如果不返回副本而是返回内部集合，这条就会失败。
     */
    @Test
    @DisplayName("findAll 返回的是副本，修改它不影响内部数据")
    void findAll_shouldReturnCopy() {
        todoService.add("A");

        List<Todo> first = todoService.findAll();
        first.clear();                                  // 恶意清空返回的列表

        assertEquals(1, todoService.findAll().size(), "内部数据不应被外部修改影响");
    }

    // ==================== findById ====================

    @Test
    @DisplayName("findById：存在时返回 Optional.of")
    void findById_found() {
        Todo created = todoService.add("查我");

        Optional<Todo> found = todoService.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("查我", found.get().getTitle());
    }

    @Test
    @DisplayName("findById：不存在时返回 Optional.empty（而不是 null）")
    void findById_missing() {
        Optional<Todo> found = todoService.findById(999L);
        assertFalse(found.isPresent());
    }

    // ==================== toggle ====================

    @Test
    @DisplayName("toggle 应把未完成变完成，再 toggle 变回未完成")
    void toggle_shouldFlipBackAndForth() {
        Todo created = todoService.add("切换我");
        assertFalse(created.isDone(), "新建时应该是未完成");

        // 注意语义：toggle 返回的是「切换之后」的状态，不是切换之前的。
        // 写断言前一定要先确认方法返回值的含义 —— 这里最容易写出「期望错误」的测试。
        assertTrue(todoService.toggle(created.getId()).get().isDone(), "第 1 次切换 -> 已完成");
        assertFalse(todoService.toggle(created.getId()).get().isDone(), "第 2 次切换 -> 未完成");
        assertTrue(todoService.toggle(created.getId()).get().isDone(), "第 3 次切换 -> 已完成");
    }

    @Test
    @DisplayName("toggle 不存在的编号：返回空 Optional，不抛异常")
    void toggle_missing_returnsEmpty() {
        assertFalse(todoService.toggle(999L).isPresent());
    }

    // ==================== findUnfinished ====================

    @Test
    @DisplayName("findUnfinished 应过滤掉已完成的")
    void findUnfinished_shouldFilterDone() {
        todoService.add("未完成1");
        Todo doneOne = todoService.add("已完成");
        todoService.add("未完成2");

        todoService.toggle(doneOne.getId());     // 把第二条标记为完成

        List<Todo> unfinished = todoService.findUnfinished();
        assertEquals(2, unfinished.size());
        assertEquals("未完成1", unfinished.get(0).getTitle());
        assertEquals("未完成2", unfinished.get(1).getTitle());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete 已存在的返回 true，并真的删掉")
    void delete_existing() {
        Todo created = todoService.add("删我");

        assertTrue(todoService.delete(created.getId()));
        assertEquals(0, todoService.count());
        assertFalse(todoService.findById(created.getId()).isPresent());
    }

    @Test
    @DisplayName("delete 不存在的返回 false")
    void delete_missing() {
        assertFalse(todoService.delete(999L));
    }

    // ==================== 综合场景 ====================
    // 好的测试不只测单个方法，还要测「多个操作串起来」的正确性。

    @Test
    @DisplayName("综合：新增 3 条 -> 完成 1 条 -> 删 1 条 -> 剩余状态正确")
    void scenario_fullLifecycle() {
        Todo a = todoService.add("任务A");
        Todo b = todoService.add("任务B");
        Todo c = todoService.add("任务C");

        todoService.toggle(b.getId());          // B 完成
        todoService.delete(c.getId());          // C 删除

        assertEquals(2, todoService.count());
        assertEquals(1, todoService.findUnfinished().size());
        assertTrue(todoService.findById(b.getId()).get().isDone());
        assertEquals(a.getId(), todoService.findAll().get(0).getId());
    }

    @Test
    @DisplayName("clear 应清空数据并重置编号")
    void clear_shouldResetAll() {
        todoService.add("A");
        todoService.add("B");

        todoService.clear();

        assertEquals(0, todoService.count());
        assertTrue(todoService.findAll().isEmpty());
        // 编号也重置了，所以下一条又是 1
        assertEquals(1L, todoService.add("新开始").getId());
    }
}
