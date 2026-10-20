package com.example.demo.model;

/**
 * 购物车里的一行 —— 这是给前端看的「视图对象」，不是数据库表结构的照搬。
 *
 * t_cart_item 里只存 user_id / product_id / quantity；
 * 商品名、价格、emoji 是查询时 JOIN t_product 带出来的，
 * 这样前端拿到一行就能直接渲染，不用再逐个请求商品接口。
 *
 * 注意：这里刻意不含 userId —— 前端只关心自己的购物车，不需要知道用户编号。
 */
public class CartItem {

    /** 商品编号 */
    private long productId;

    /** 商品名称（JOIN 而来） */
    private String name;

    /** 现价（JOIN 而来） */
    private double price;

    /** 商品图 emoji（JOIN 而来） */
    private String emoji;

    /** 数量 */
    private int quantity;

    public CartItem() {
    }

    public CartItem(long productId, String name, double price, String emoji, int quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.emoji = emoji;
        this.quantity = quantity;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * 小计 = 单价 × 数量。
     *
     * 只有 getter 没有字段，Jackson 会把它当属性序列化出去（subtotal）——
     * 让前端少算一次，也避免前后端各算一遍算出不一样的结果。
     * 价格是钱，先放大 100 倍取整再缩回来，避免 0.1 + 0.2 = 0.30000000000000004。
     */
    public double getSubtotal() {
        return Math.round(price * quantity * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return "CartItem{productId=" + productId + ", name='" + name + "', quantity=" + quantity + "}";
    }
}
