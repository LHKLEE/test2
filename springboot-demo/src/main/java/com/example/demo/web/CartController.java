package com.example.demo.web;

import com.example.demo.model.CartItem;
import com.example.demo.model.CartSummary;
import com.example.demo.service.CartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Locale;

/**
 * 购物车接口 —— 全部需要登录，未登录统一 401。
 *
 * 接口清单（都返回「更新后的整车汇总」，前端拿到直接重绘）：
 *   GET    /api/cart                      查看
 *   POST   /api/cart/items                body: {"productId":1,"quantity":2} 加入（已有则累加）
 *   PUT    /api/cart/items/{productId}    body: {"quantity":3}  直接改数量（0 等于删除）
 *   DELETE /api/cart/items/{productId}    删除一行
 *   DELETE /api/cart                      清空
 *   POST   /api/cart/merge                body: {"items":[{"productId":1,"quantity":2}]}
 *                                         把游客存在浏览器里的购物车合并进来
 *   POST   /api/cart/checkout             结算（演示用：清空购物车并返回订单摘要）
 *
 * 为什么每个方法都要 require(session)？
 *   购物车接口拿到的只有会话，没有「用户编号」参数。绝对不能让前端传 userId ——
 *   否则任何人改一下参数就能看别人的购物车。用户身份只能由服务端从会话里取，
 *   这是这类接口最重要的一条安全纪律。
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** 查看当前用户的购物车 */
    @GetMapping
    public CartSummary view(HttpSession session) {
        return cartService.view(SessionUser.require(session));
    }

    /** 加入购物车；quantity 不传默认为 1 */
    @PostMapping("/items")
    public CartSummary add(@RequestBody AddRequest body, HttpSession session) {
        long userId = SessionUser.require(session);
        if (body == null) {
            throw new IllegalArgumentException("请求内容不能为空");
        }
        int quantity = body.getQuantity() == null ? 1 : body.getQuantity();
        return cartService.add(userId, body.getProductId(), quantity);
    }

    /** 直接设置数量（购物车里的 ＋ － 走这里，比「加一件/减一件」更不容易出现前后端数量不一致） */
    @PutMapping("/items/{productId}")
    public CartSummary setQuantity(@PathVariable("productId") long productId,
                                   @RequestBody QuantityRequest body,
                                   HttpSession session) {
        long userId = SessionUser.require(session);
        if (body == null || body.getQuantity() == null) {
            throw new IllegalArgumentException("请提供 quantity");
        }
        return cartService.setQuantity(userId, productId, body.getQuantity());
    }

    /** 删除一行 */
    @DeleteMapping("/items/{productId}")
    public CartSummary remove(@PathVariable("productId") long productId, HttpSession session) {
        return cartService.remove(SessionUser.require(session), productId);
    }

    /** 清空购物车 */
    @DeleteMapping
    public CartSummary clear(HttpSession session) {
        return cartService.clear(SessionUser.require(session));
    }

    /** 把游客购物车合并到登录账号里（登录成功后前端立刻调用） */
    @PostMapping("/merge")
    public CartSummary merge(@RequestBody MergeRequest body, HttpSession session) {
        long userId = SessionUser.require(session);
        List<CartItem> items = (body == null) ? null : body.getItems();
        return cartService.merge(userId, items);
    }

    /**
     * 结算。真实项目这里要创建订单、扣库存、对接支付；
     * 教学 Demo 只做到「校验非空 -> 算钱 -> 清空购物车」。
     */
    @PostMapping("/checkout")
    public CheckoutResult checkout(HttpSession session) {
        long userId = SessionUser.require(session);
        CartSummary cart = cartService.view(userId);
        if (cart.getQuantity() == 0) {
            throw new IllegalArgumentException("购物车是空的，先挑点东西吧");
        }
        cartService.clear(userId);
        String message = String.format(Locale.ROOT,
                "下单成功，已模拟支付 ¥%.2f（%d 件）", cart.getTotal(), cart.getQuantity());
        return new CheckoutResult(message, cart.getTotal(), cart.getQuantity());
    }

    /** 加入购物车的请求体 */
    public static class AddRequest {

        private Long productId;
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    /** 修改数量的请求体 */
    public static class QuantityRequest {

        private Integer quantity;

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    /** 合并游客购物车的请求体 */
    public static class MergeRequest {

        private List<CartItem> items;

        public List<CartItem> getItems() {
            return items;
        }

        public void setItems(List<CartItem> items) {
            this.items = items;
        }
    }

    /** 结算结果 */
    public static class CheckoutResult {

        private final String message;
        private final double total;
        private final int quantity;

        CheckoutResult(String message, double total, int quantity) {
            this.message = message;
            this.total = total;
            this.quantity = quantity;
        }

        public String getMessage() {
            return message;
        }

        public double getTotal() {
            return total;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
