package com.example.demo.service;

import com.example.demo.model.Todo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 待办事项服务 —— 用来学习「带状态的业务逻辑怎么测」。
 *
 * 和 CalculatorService 的区别：
 *   它有内部状态（一个 Map），所以每个测试方法之间会互相影响。
 *   解决办法是在 @BeforeEach 里调用 clear()，保证每个测试都从干净状态开始。
 *
 * 数据存在内存里，重启应用就没了 —— 这是刻意的，为了不引入数据库，
 * 让测试保持简单。
 */
@Service
public class TodoService {

    /** 用 LinkedHashMap 保持插入顺序，这样列表顺序是稳定的、可断言的 */
    private final Map<Long, Todo> store = new LinkedHashMap<>();

    /** 自增编号。AtomicLong 保证并发下也不会重复 */
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * 新增一条待办。
     *
     * @param title 待办内容，不能为空白
     * @throws IllegalArgumentException 当 title 为 null 或全是空白
     */
    public synchronized Todo add(String title) {
        if (isBlank(title)) {
            throw new IllegalArgumentException("待办内容不能为空");
        }
        long id = sequence.incrementAndGet();
        Todo todo = new Todo(id, strip(title), false);
        store.put(id, todo);
        return todo;
    }

    /** 按插入顺序返回全部待办 */
    public synchronized List<Todo> findAll() {
        return new ArrayList<>(store.values());
    }

    /** 只返回未完成的 */
    public synchronized List<Todo> findUnfinished() {
        List<Todo> result = new ArrayList<>();
        for (Todo todo : store.values()) {
            if (!todo.isDone()) {
                result.add(todo);
            }
        }
        return result;
    }

    /** 按编号查找；找不到返回空 Optional，而不是 null */
    public synchronized Optional<Todo> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * 切换完成状态（完成 &lt;-&gt; 未完成）。
     *
     * @return 切换后的对象；编号不存在时返回空 Optional
     */
    public synchronized Optional<Todo> toggle(long id) {
        Todo todo = store.get(id);
        if (todo == null) {
            return Optional.empty();
        }
        todo.setDone(!todo.isDone());
        return Optional.of(todo);
    }

    /**
     * 删除一条。
     *
     * @return 删除成功返回 true；编号不存在返回 false
     */
    public synchronized boolean delete(long id) {
        return store.remove(id) != null;
    }

    /** 当前条数 */
    public synchronized int count() {
        return store.size();
    }

    /**
     * 清空所有数据。
     *
     * 这个方法主要是为测试准备的：@BeforeEach 里调用它，
     * 就能让每个测试方法互不干扰。
     */
    public synchronized void clear() {
        store.clear();
        sequence.set(0);
    }

    // ==================== 私有工具方法 ====================
    // 下面三个方法是为了修一个被测试发现的缺陷，详见 README「测试发现了什么」。

    /**
     * 判断字符串是否为「空白」（null、空串、或全部由空白字符组成）。
     *
     * 为什么不用 {@code title.trim().isEmpty()}？
     *   {@code trim()} 只去除码点 &lt;= U+0020 的字符，而中文输入法常见的
     *   全角空格 {@code '\u3000'} 不在这个范围内。结果是用户能创建出一条
     *   「看起来是空的」待办。这是 TodoServiceEdgeCaseTest 抓出来的真实缺陷。
     *
     * 注：Java 11 起可以直接用 {@code String.isBlank()}，本项目是 Java 8。
     */
    private static boolean isBlank(String text) {
        if (text == null) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!isBlankChar(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 去掉首尾空白（包含全角空格）。
     * Java 8 没有 {@code String.strip()}，所以用和 isBlank 一致的标准自己实现。
     */
    private static String strip(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && isBlankChar(text.charAt(start))) {
            start++;
        }
        while (end > start && isBlankChar(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    /** 单个字符是否算空白：半角空白 + 全角空格等 Unicode 空格 */
    private static boolean isBlankChar(char c) {
        // isWhitespace 覆盖空格/制表符/换行；isSpaceChar 覆盖全角空格等
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
}
