package com.example.demo.service;

import org.springframework.stereotype.Service;

/**
 * 计算器服务 —— 用来学习「最基础的单元测试」。
 *
 * 为什么它最容易测？
 *   1. 没有状态：每次调用结果只取决于入参，反复调用结果一致
 *   2. 没有依赖：不需要数据库、网络、Spring 容器
 *   3. 结果确定：可以用 assertEquals 直接断言
 *
 * 因此测试时可以直接 new CalculatorService()，不需要启动 Spring。
 */
@Service
public class CalculatorService {

    /** 加法 */
    public int add(int a, int b) {
        return a + b;
    }

    /** 减法 */
    public int subtract(int a, int b) {
        return a - b;
    }

    /** 乘法 */
    public int multiply(int a, int b) {
        return a * b;
    }

    /**
     * 除法。除数为 0 时抛出异常 —— 这是一个很适合练习「异常断言」的点。
     *
     * @throws IllegalArgumentException 当 b 为 0
     */
    public int divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为 0");
        }
        return a / b;
    }

    /** 判断是否为偶数。参数多样，适合练习「参数化测试」 */
    public boolean isEven(int number) {
        return number % 2 == 0;
    }

    /**
     * 求数组中的最大值 —— 适合练习「边界值测试」。
     *
     * @throws IllegalArgumentException 当数组为 null 或长度为 0
     */
    public int max(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("数组不能为空");
        }
        int result = numbers[0];
        for (int n : numbers) {
            if (n > result) {
                result = n;
            }
        }
        return result;
    }
}
