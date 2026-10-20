package com.example.demo.service;

import com.example.demo.model.CartItem;
import com.example.demo.model.CartSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 购物车服务 —— 每个登录用户一辆车，存在 t_cart_item 表。
 *
 * 和商品一样用 JdbcTemplate 写 SQL。所有方法都要求传 userId，
 * 「当前是谁」这件事由 Controller 从会话里取（见 {@code web.SessionUser}），
 * 服务层不认识 HTTP，这样它才好测、也好复用。
 *
 * 每个写操作都返回「更新后的整车汇总」，前端拿到就能直接重绘。
 */
@Service
public class CartService {

    /** 同一件商品最多买几件，防止误点把数量点成几百 */
    public static final int MAX_QUANTITY = 99;

    /**
     * 查询购物车时把商品信息 JOIN 进来。
     *
     * 为什么用 JOIN 而不是先查购物车再逐个查商品？
     * 前者是 1 条 SQL，后者是 1 + N 条 —— 这就是常说的 N+1 查询问题。
     *
     * 排序：最近改动的排在前面，符合用户「刚加的东西想马上看到」的直觉。
     */
    private static final String LIST_SQL =
            "SELECT c.product_id, c.quantity, p.name, p.price, p.emoji "
          + "FROM t_cart_item c "
          + "JOIN t_product p ON p.id = c.product_id "
          + "WHERE c.user_id = ? "
          + "ORDER BY c.updated_at DESC, c.id DESC";

    private final JdbcTemplate jdbc;

    private final RowMapper<CartItem> rowMapper = (rs, rowNum) -> new CartItem(
            rs.getLong("product_id"),
            rs.getString("name"),
            rs.getDouble("price"),
            rs.getString("emoji"),
            rs.getInt("quantity"));

    public CartService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 查看购物车 */
    public CartSummary view(long userId) {
        return CartSummary.of(jdbc.query(LIST_SQL, rowMapper, userId));
    }

    /**
     * 加入购物车。已经在该用户车里的话，数量累加。
     *
     * SQL 里的 ON DUPLICATE KEY UPDATE 依赖 (user_id, product_id) 这个唯一索引：
     *   插入成功 —— 说明车里本来没有这件商品；
     *   撞唯一索引 —— 转成 UPDATE，把数量加上去。
     * 一条语句搞定「有则累加、无则新增」，不用先查再判断。
     *
     * 这里用 LEAST(quantity + ?, 99) 而不是 MySQL 8 已废弃的 VALUES(quantity) 写法，
     * 5.7 和 8.0 都能跑、都不报警告。
     */
    public CartSummary add(long userId, long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于 0");
        }
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("单次最多加入 " + MAX_QUANTITY + " 件");
        }
        if (!productExists(productId)) {
            throw new IllegalArgumentException("商品不存在：" + productId);
        }

        jdbc.update("INSERT INTO t_cart_item (user_id, product_id, quantity) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE quantity = LEAST(quantity + ?, " + MAX_QUANTITY + ")",
                userId, productId, quantity, quantity);

        return view(userId);
    }

    /**
     * 直接设置某件商品的数量（购物车里的 ＋ － 按钮走这里）。
     * 数量设为 0 或负数 = 删除这一行。
     */
    public CartSummary setQuantity(long userId, long productId, int quantity) {
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("单件商品最多 " + MAX_QUANTITY + " 件");
        }
        if (quantity <= 0) {
            return remove(userId, productId);
        }
        jdbc.update("UPDATE t_cart_item SET quantity = ? WHERE user_id = ? AND product_id = ?",
                quantity, userId, productId);
        return view(userId);
    }

    /** 删掉一行 */
    public CartSummary remove(long userId, long productId) {
        jdbc.update("DELETE FROM t_cart_item WHERE user_id = ? AND product_id = ?", userId, productId);
        return view(userId);
    }

    /** 清空整车 */
    public CartSummary clear(long userId) {
        jdbc.update("DELETE FROM t_cart_item WHERE user_id = ?", userId);
        return CartSummary.empty();
    }

    /**
     * 把「游客购物车」合并进来。
     *
     * 场景：没登录时加的购物车存在浏览器 localStorage 里，一旦登录，
     * 不能把那几件商品丢了，所以前端把本地清单发过来，这里逐件累加。
     * 单件商品不存在（比如已下架）就跳过，不影响其他商品合并。
     */
    public CartSummary merge(long userId, List<CartItem> guestItems) {
        if (guestItems != null) {
            for (CartItem item : guestItems) {
                if (item == null || item.getQuantity() <= 0 || !productExists(item.getProductId())) {
                    continue;
                }
                int quantity = Math.min(item.getQuantity(), MAX_QUANTITY);
                jdbc.update("INSERT INTO t_cart_item (user_id, product_id, quantity) VALUES (?, ?, ?) "
                                + "ON DUPLICATE KEY UPDATE quantity = LEAST(quantity + ?, " + MAX_QUANTITY + ")",
                        userId, item.getProductId(), quantity, quantity);
            }
        }
        return view(userId);
    }

    /** 商品是否存在（外键也会拦，但提前判断能给出更友好的报错） */
    private boolean productExists(long productId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_product WHERE id = ?", Integer.class, productId);
        return n != null && n > 0;
    }
}
