package com.example.demo.service;

import com.example.demo.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 商品服务 —— 数据来自 MySQL 的 t_product 表，SQL 由 JdbcTemplate 执行。
 *
 * 和以前的区别：以前商品写死在 Java 里（进程重启就回来的「内存版」），
 * 现在改成查库，改了数据不用重新编译。
 *
 * 但对外的方法签名基本都保留了（findAll / findByCategory / search / findById / categories），
 * 只多了一个 find(category, keyword) 用来把两个筛选条件合并成一条 SQL ——
 * 所以 ProductController 只需要把 list() 改成调用它，其余方法都不用动。
 * 这就是「接口稳定、实现可替换」。
 */
@Service
public class ProductService {

    /** 「全部」这个伪分类，前端分类条的第一个 */
    public static final String ALL_CATEGORY = "全部";

    /** 查表用的列，抽出来避免各处写不一致 */
    private static final String COLUMNS =
            "id, name, price, original_price, category, emoji, rating, sales, tag, description";

    private final JdbcTemplate jdbc;

    /** 把一行结果变成 Product 对象 */
    private final RowMapper<Product> rowMapper = (rs, rowNum) -> new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getDouble("price"),
            rs.getDouble("original_price"),
            rs.getString("category"),
            rs.getString("emoji"),
            rs.getDouble("rating"),
            rs.getInt("sales"),
            rs.getString("tag"),
            rs.getString("description"));

    public ProductService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 查询全部商品，按编号排序（等于保持数据文件里的顺序） */
    public List<Product> findAll() {
        return find(null, null);
    }

    /** 按分类筛选。「全部」或空白表示不筛选 */
    public List<Product> findByCategory(String category) {
        return find(category, null);
    }

    /** 按关键词搜索名称与卖点。空白表示不筛选 */
    public List<Product> search(String keyword) {
        return find(null, keyword);
    }

    /**
     * 分类 + 关键词一起查，两个条件都是可选的 —— 主页那个搜索框走的就是这里。
     *
     * 为什么把两个条件合并进一条 SQL，而不是「先按分类查一遍、再按关键词查一遍、取交集」？
     *
     *   取交集得用 {@code List.retainAll()}，而它依赖元素的 {@code equals()}。
     *   Product 没有重写 equals()，比的其实是对象地址 —— 两条 SQL 各自 new 出来
     *   一批 Product 实例，地址全不一样，交集就永远是空集，**搜什么都搜不到**。
     *   （这个坑在内存版里不会暴露：那时两次查询返回的是同一批对象，地址相同。）
     *
     * 交给数据库在 WHERE 里同时判断：既没有 equals 的问题，也少一次查询。
     *
     * 大小写：t_product 用 utf8mb4_general_ci 排序规则（ci = case insensitive），
     * 所以 LIKE 天然不区分大小写，不用像内存版那样手动 toLowerCase。
     *
     * 转义：LIKE 里的 % 和 _ 是通配符。用户搜「100%」时若不转义，
     * 就变成「匹配任意内容」，会搜出一堆不相关的东西，所以这里手动加反斜杠转义。
     */
    public List<Product> find(String category, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM t_product");
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        String cat = Texts.strip(category);
        if (!cat.isEmpty() && !ALL_CATEGORY.equals(cat)) {
            conditions.add("category = ?");
            params.add(cat);
        }

        String kw = Texts.strip(keyword);
        if (!kw.isEmpty()) {
            // 商品名或卖点命中都算
            conditions.add("(name LIKE ? OR description LIKE ?)");
            String like = "%" + escapeLike(kw) + "%";
            params.add(like);
            params.add(like);
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY id");

        return jdbc.query(sql.toString(), rowMapper, params.toArray());
    }

    /** 查询单件商品，不存在返回空 Optional */
    public Optional<Product> findById(long id) {
        List<Product> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM t_product WHERE id = ?", rowMapper, id);
        return rows.isEmpty() ? Optional.<Product>empty() : Optional.of(rows.get(0));
    }

    /**
     * 返回分类列表，第一个固定是「全部」。
     * 分类从数据里查（DISTINCT），所以往库里加一个新分类的商品，页面分类条会自动多一项。
     */
    public List<String> categories() {
        List<String> result = new ArrayList<>();
        result.add(ALL_CATEGORY);
        result.addAll(jdbc.queryForList(
                "SELECT DISTINCT category FROM t_product ORDER BY category", String.class));
        return result;
    }

    /** 商品总数 */
    public int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM t_product", Integer.class);
        return n == null ? 0 : n;
    }

    /** 转义 LIKE 的通配符，让用户输入被当成普通文本 */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\")
                   .replace("%", "\\%")
                   .replace("_", "\\_");
    }
}
