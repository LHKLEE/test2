package com.example.demo.service;

import com.example.demo.model.Todo;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 边界值与特殊输入的测试，顺带演示 AssertJ 的几种高级断言。
 *
 * 为什么要专门测「边界」？
 *   正常路径（输入一个普通标题）几乎不会出错。真正出 bug 的地方是：
 *     空、null、超长、特殊字符、全角字符、并发……
 *   一个方法最容易被漏测的就是这些。经验法则：写完正常用例后，
 *   强制自己再想三个「奇怪但合法」的输入。
 *
 * 本文件用 AssertJ 而不是 JUnit 原生断言，因为：
 *   1. 链式写法更接近自然语言：assertThat(x).isEqualTo(y)
 *   2. 失败信息更详细，能直接看出「期望 vs 实际」
 *   3. 集合断言强大得多
 */
@DisplayName("TodoService 边界值与 AssertJ 断言")
class TodoServiceEdgeCaseTest {

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService();
    }

    // ==================== 1. 超长输入 ====================

    @Test
    @DisplayName("超长标题（10000 字符）应能完整保存，不被截断")
    void add_veryLongTitle() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append('a');
        }
        String longTitle = sb.toString();

        Todo created = todoService.add(longTitle);

        assertThat(created.getTitle()).hasSize(10000);
        assertThat(todoService.findById(created.getId()).get().getTitle()).isEqualTo(longTitle);
    }

    // ==================== 2. 特殊字符 ====================

    @Test
    @DisplayName("中文 / emoji / 引号 / 换行 都应原样保存")
    void add_specialCharacters() {
        String title = "中文 emoji 🎉 与 \"引号\" 和\n换行 & <标签>";

        Todo created = todoService.add(title);

        // 注意：换行在首尾才会被 trim 影响，这里是中间，所以应原样保留
        assertThat(created.getTitle()).isEqualTo(title);
    }

    // ==================== 3. 各种「空白」输入 ====================

    @Test
    @DisplayName("半角空白（空格/制表符/换行）都应被拒绝")
    void add_asciiWhitespace_shouldThrow() {
        assertThatThrownBy(() -> todoService.add(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("待办内容不能为空");

        assertThatThrownBy(() -> todoService.add("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> todoService.add("\t"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> todoService.add("\n"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("全角空格（\\u3000）也应被视为空白 —— 中文输入法很容易打出来")
    void add_fullWidthSpace_shouldThrow() {
        // String.trim() 只去掉码点 <= U+0020 的字符，\u3000（全角空格）不在其中。
        //
        // 也就是说：如果服务端只用了 trim()，下面这条断言就会失败 ——
        // 用户会成功创建出一条「看起来是空的」待办。
        //
        // 这正是单元测试的价值：它能把这种「需求说不清、代码没考虑到」的
        // 模糊地带，变成一个明确的、会失败的断言。
        assertThatThrownBy(() -> todoService.add("\u3000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("待办内容不能为空");
    }

    // ==================== 4. AssertJ 链式断言 ====================

    @Test
    @DisplayName("集合断言：hasSize / extracting / containsExactly")
    void assertj_collectionAssertions() {
        todoService.add("A");
        todoService.add("B");
        todoService.add("C");

        assertThat(todoService.findAll())
                .hasSize(3)                          // 长度
                .extracting(Todo::getTitle)          // 抽取字段
                .containsExactly("A", "B", "C");     // 顺序和内容都要对
    }

    @Test
    @DisplayName("元组断言：一次比较多个字段，比逐个 assertEquals 更紧凑")
    void assertj_tupleAssertions() {
        todoService.add("A");
        Todo b = todoService.add("B");
        todoService.toggle(b.getId());

        // 同时断言 id / title / done 三个字段
        assertThat(todoService.findAll())
                .extracting(Todo::getId, Todo::getTitle, Todo::isDone)
                .containsExactly(
                        tuple(1L, "A", false),
                        tuple(2L, "B", true));
    }

    @Test
    @DisplayName("对象断言：returns 直接断言某个 getter 的返回值")
    void assertj_objectAssertions() {
        Todo created = todoService.add("  带空格  ");

        assertThat(created.getTitle()).isEqualTo("带空格");
        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.isDone()).isFalse();
    }

    // ==================== 5. 一个真实的「坑」 ====================

    @Test
    @DisplayName("坑：Todo 没有重写 equals，所以内容相同的两个对象并不相等")
    void equals_isNotOverridden() {
        Todo a = new Todo(1L, "同样的内容", false);
        Todo b = new Todo(1L, "同样的内容", false);

        // 下面这行会失败！因为 Todo 没重写 equals()，
        // assertEquals / isEqualTo 比较的是「是不是同一个对象」（引用地址），而不是内容。
        // assertThat(a).isEqualTo(b);

        // 解决方案一：逐字段断言（最直白）
        assertThat(a.getId()).isEqualTo(b.getId());
        assertThat(a.getTitle()).isEqualTo(b.getTitle());
        assertThat(a.isDone()).isEqualTo(b.isDone());

        // 解决方案二：AssertJ 的递归比较，自动逐字段比对（推荐，字段多时省事）
        assertThat(a).usingRecursiveComparison().isEqualTo(b);

        // 解决方案三（最彻底）：给 Todo 加上 equals() 和 hashCode()，
        // 之后就可以直接 assertThat(a).isEqualTo(b) 了。这也是一个可以做的练习。
    }

    // ==================== 6. 软断言 ====================

    @Test
    @DisplayName("SoftAssertions：先收集全部失败，最后统一报告")
    void softAssertions_collectAll() {
        Todo created = todoService.add("软断言");

        // 普通断言：第一条失败就抛出，后面的根本没机会执行。
        // 软断言：全部跑完，最后 assertAll() 一次性列出所有失败项。
        // 想「一轮就把问题看全」时非常有用。
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(created.getId()).isEqualTo(1L);
        softly.assertThat(created.getTitle()).isEqualTo("软断言");
        softly.assertThat(created.isDone()).isFalse();
        softly.assertAll();   // 别忘了这一句，否则断言不会被执行
    }
}
