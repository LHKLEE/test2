package com.example.demo.model;

/**
 * 一件商品。普通 Java 对象（POJO）。
 *
 * 需要有 getter，Jackson 才能把它序列化成 JSON 返回给前端；
 * 需要有 setter，Jackson 才能把前端传来的 JSON 反序列化成对象。
 */
public class Product {

    /** 唯一编号，对应 t_product.id（数据由 sql/init.sql 灌入，不由程序分配） */
    private long id;

    /** 商品名称 */
    private String name;

    /** 现价（单位：元） */
    private double price;

    /** 原价（单位：元）。为 0 表示不打折 */
    private double originalPrice;

    /** 所属分类，如「数码」「家居」 */
    private String category;

    /** 用 emoji 充当商品图，避免依赖外部图片资源 */
    private String emoji;

    /** 评分，0 ~ 5 */
    private double rating;

    /** 累计销量 */
    private int sales;

    /** 促销标签，如「限时直降」「新品」；可为 null */
    private String tag;

    /** 一句话卖点 */
    private String description;

    /** 无参构造：Jackson 反序列化时需要 */
    public Product() {
    }

    public Product(long id, String name, double price, double originalPrice,
                   String category, String emoji, double rating, int sales,
                   String tag, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.category = category;
        this.emoji = emoji;
        this.rating = rating;
        this.sales = sales;
        this.tag = tag;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** 折扣率百分比，如 75 表示打到 7.5 折。不打折时返回 100 */
    public int getDiscountPercent() {
        if (originalPrice <= 0 || price >= originalPrice) {
            return 100;
        }
        return (int) Math.round(price / originalPrice * 100);
    }

    /** 方便测试失败时看清对象内容 */
    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price
                + ", category='" + category + "'}";
    }
}
