package com.example.demo.web;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 商品相关的只读 REST 接口 —— 商城主页的数据来源。
 *
 * 接口清单：
 *   GET /api/products                      查询全部
 *   GET /api/products?category=数码         按分类筛选
 *   GET /api/products?keyword=耳机          关键词搜索
 *   GET /api/products/categories           返回全部分类
 *   GET /api/products/{id}                 查询单条（不存在返回 404）
 *
 * 与 TodoController 不同，这里只读取数据，所以没有写操作的接口，
 * 也用不着全局异常处理器。购物车逻辑放在前端（localStorage）实现。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /** 构造器注入 */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 商品列表：分类筛选 + 关键词搜索，两个条件可以同时传（此时取交集）。
     *
     * 交集交给数据库一条 SQL 查完（见 {@code ProductService#find}）。
     * 这里以前是拿两个 List 自己做 {@code retainAll}，那个写法依赖 Product.equals()，
     * 而 Product 并没有重写它 —— 两条 SQL 查出来的对象地址不同，交集永远是空集，
     * 于是「搜索什么都搜不到」。
     * 教训：别用 equals 去表达业务上的「同一件商品」，能让数据库判断的就别搬到内存里。
     */
    @GetMapping
    public List<Product> list(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return productService.find(category, keyword);
    }

    /** 全部分类 */
    @GetMapping("/categories")
    public List<String> categories() {
        return productService.categories();
    }

    /** 单件商品，不存在返回 404 */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getOne(@PathVariable("id") long id) {
        Optional<Product> found = productService.findById(id);
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
