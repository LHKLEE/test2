package com.example.demo.model;

/**
 * 一条待办事项。就是一个普通 Java 对象（POJO）。
 *
 * 需要有 getter，Jackson 才能把它序列化成 JSON 返回给前端；
 * 需要有 setter，Jackson 才能把前端传来的 JSON 反序列化成对象。
 */
public class Todo {

    /** 唯一编号，由 TodoService 分配 */
    private long id;

    /** 待办内容 */
    private String title;

    /** 是否已完成 */
    private boolean done;

    /** 无参构造：Jackson 反序列化时需要 */
    public Todo() {
    }

    public Todo(long id, String title, boolean done) {
        this.id = id;
        this.title = title;
        this.done = done;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    /** 方便测试失败时看清对象内容 */
    @Override
    public String toString() {
        return "Todo{id=" + id + ", title='" + title + "', done=" + done + "}";
    }
}
