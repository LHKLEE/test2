package com.example.demo.web;

import javax.servlet.http.HttpSession;

/**
 * 会话里的「当前登录用户」。
 *
 * 登录成功后只在 session 里存一个用户编号，不存整个 User 对象：
 *   - 会话是服务端内存，存得越少越省；更关键的是不会存下一份可能过期的副本
 *   - 每次需要用户信息时按 id 回查数据库，改昵称之后立刻生效
 *
 * 为什么不用 Spring Security？它是「正确的答案」，但对一个教学 Demo 来说
 * 会引入过滤器链、登录页、权限表达式等一大堆概念，反而看不清登录本身是怎么回事。
 * 这里用最朴素的 HttpSession，Servlet 容器会自动发一个 JSESSIONID（本项目叫 MALLSESSION）Cookie。
 *
 * 注意：会话存在内存里，所以重启应用 = 所有人被登出。真实项目会存 Redis 或用 JWT。
 */
final class SessionUser {

    /** 存在 session 里的键名 */
    private static final String ATTR = "MALL_UID";

    private SessionUser() {
    }

    /** 登录：把用户编号写进会话 */
    static void login(HttpSession session, long userId) {
        session.setAttribute(ATTR, userId);
    }

    /** 退出：把编号从会话里摘掉（不销毁整个会话，避免把游客购物车等临时状态一起弄丢） */
    static void logout(HttpSession session) {
        session.removeAttribute(ATTR);
    }

    /** 当前用户编号，未登录返回 null */
    static Long userId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(ATTR);
        // 用 Number 而不是强转 Long：会话属性在某些容器/序列化下可能变回 Integer
        return (value instanceof Number) ? ((Number) value).longValue() : null;
    }

    /**
     * 取当前用户编号，未登录直接抛 401。
     * 有了它，购物车那几个接口开头都只需要一行 require(session)。
     */
    static long require(HttpSession session) {
        Long userId = userId(session);
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        return userId;
    }
}
