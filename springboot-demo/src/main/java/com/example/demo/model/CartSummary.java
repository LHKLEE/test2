package com.example.demo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 购物车汇总 —— 每次对购物车的增删改，接口都返回一整个汇总。
 *
 * 这样做的好处：前端不用自己维护「加了一件之后角标该显示几」，
 * 服务端算完一次性返回，前端只管重新渲染，不容易出现前后端数据不一致。
 */
public class CartSummary {

    /** 所有行 */
    private List<CartItem> items = new ArrayList<>();

    /** 总件数（各行数量之和） */
    private int quantity;

    /** 总金额（元） */
    private double total;

    public CartSummary() {
    }

    /** 由行列表算出总件数和总金额 */
    public static CartSummary of(List<CartItem> items) {
        CartSummary summary = new CartSummary();
        summary.items = items == null ? Collections.<CartItem>emptyList() : items;

        int quantity = 0;
        double total = 0.0;
        for (CartItem item : summary.items) {
            quantity += item.getQuantity();
            total += item.getSubtotal();
        }
        summary.quantity = quantity;
        summary.total = Math.round(total * 100.0) / 100.0;
        return summary;
    }

    /** 空购物车 */
    public static CartSummary empty() {
        return new CartSummary();
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
