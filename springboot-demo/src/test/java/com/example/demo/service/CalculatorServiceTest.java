package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【第一层】最基础的单元测试
 *
 * 特点：完全不启动 Spring，直接 new 一个对象来测，跑得飞快（毫秒级）。
 * 判断标准：如果一个类「不用 Spring 也能 new 出来」，就应该这样测。
 *
 * 本文件演示了 5 个最常用的测试套路：
 *   1. @BeforeEach  每个测试前重建对象，保证互不干扰
 *   2. assertEquals 断言返回值
 *   3. assertThrows 断言异常
 *   4. assertAll    一次报告所有失败
 *   5. @ParameterizedTest 参数化测试，一组数据跑同一个逻辑
 */
@DisplayName("CalculatorService（纯单元测试，不启动 Spring）")
class CalculatorServiceTest {

    private CalculatorService calculator;

    /**
     * 每个 @Test 执行前都会先跑一遍这里。
     * 好处：上一个测试改了对象状态，也不会影响下一个测试。
     */
    @BeforeEach
    void setUp() {
        calculator = new CalculatorService();
    }

    // ==================== 基础断言 ====================

    @Test
    @DisplayName("加法：1 + 2 = 3")
    void add_shouldReturnSum() {
        // 第一个参数是「期望值」，第二个是「实际值」。顺序反了报错信息会很别扭。
        assertEquals(3, calculator.add(1, 2));
    }

    @Test
    @DisplayName("减法：5 - 3 = 2")
    void subtract_shouldReturnDifference() {
        assertEquals(2, calculator.subtract(5, 3));
    }

    @Test
    @DisplayName("乘法：2 * 3 = 6")
    void multiply_shouldReturnProduct() {
        assertEquals(6, calculator.multiply(2, 3));
    }

    @Test
    @DisplayName("除法：6 / 3 = 2")
    void divide_shouldReturnQuotient() {
        assertEquals(2, calculator.divide(6, 3));
    }

    // ==================== 异常断言 ====================

    @Test
    @DisplayName("除以 0 应抛 IllegalArgumentException，且提示信息正确")
    void divide_byZero_shouldThrow() {
        // assertThrows 返回捕获到的异常对象，可以继续断言它的消息
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> calculator.divide(1, 0)   // 要执行的代码写成 lambda
        );
        assertEquals("除数不能为 0", ex.getMessage());
    }

    // ==================== 分组断言 ====================

    @Test
    @DisplayName("assertAll：多个断言一起执行，失败也会全部报告")
    void assertAll_demo() {
        // 普通写法：第一个断言失败，后面的就不会执行了，你得反复跑很多次才能修完
        // assertAll 会把所有失败一次性列出来
        assertAll("计算器基础运算",
                () -> assertEquals(5, calculator.add(2, 3)),
                () -> assertEquals(1, calculator.subtract(3, 2)),
                () -> assertEquals(6, calculator.multiply(2, 3)),
                () -> assertEquals(2, calculator.divide(4, 2))
        );
    }

    // ==================== 布尔断言 ====================

    @Test
    @DisplayName("isEven：4 是偶数")
    void isEven_trueForEven() {
        assertTrue(calculator.isEven(4));
    }

    @Test
    @DisplayName("isEven：用于负数也成立（-4 是偶数）")
    void isEven_trueForNegativeEven() {
        assertTrue(calculator.isEven(-4));
    }

    // ==================== 参数化测试 ====================

    /**
     * 与其为每个数字写一个 @Test，不如用 @ParameterizedTest 写一个。
     * 测试报告里会显示成独立的多个用例，失败时能看出是哪个参数挂了。
     */
    @ParameterizedTest(name = "isEven({0}) 应为 true")
    @ValueSource(ints = {0, 2, 4, 100, -2, -100})
    void isEven_shouldBeTrueForEvenNumbers(int number) {
        assertTrue(calculator.isEven(number));
    }

    @ParameterizedTest(name = "isEven({0}) 应为 false")
    @ValueSource(ints = {1, 3, 99, -1, -99})
    void isEven_shouldBeFalseForOddNumbers(int number) {
        assertFalse(calculator.isEven(number));
    }

    /** @CsvSource 适合「多个入参 + 一个期望结果」的场景，用逗号分隔 */
    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
            "1, 2, 3",
            "0, 0, 0",
            "-1, 1, 0",
            "100, 200, 300"
    })
    void add_parameterized(int a, int b, int expected) {
        assertEquals(expected, calculator.add(a, b));
    }

    // ==================== 边界值测试 ====================
    // 经验：一个方法最容易被漏测的就是「空 / null / 0 / 只有一个元素」这些边界。

    @Test
    @DisplayName("max：单个元素时返回它自己")
    void max_singleElement() {
        assertEquals(7, calculator.max(new int[]{7}));
    }

    @Test
    @DisplayName("max：正常数组返回最大值")
    void max_normalArray() {
        assertEquals(9, calculator.max(new int[]{3, 9, 1, 9, 4}));
    }

    @Test
    @DisplayName("max：空数组抛异常")
    void max_emptyArray_shouldThrow() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> calculator.max(new int[0])
        );
        assertEquals("数组不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("max：null 抛异常")
    void max_null_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> calculator.max(null));
    }

    // ==================== 用 @Nested 把测试分组 ====================
    // 测试多了以后，用嵌套类分组，报告里会显示成层级结构，更容易定位。

    @Nested
    @DisplayName("当除数不为 0 时")
    class WhenDivisorIsNotZero {

        @Test
        @DisplayName("7 / 2 取整为 3（整数除法会截断小数）")
        void divide_truncates() {
            assertEquals(3, calculator.divide(7, 2));
        }

        @Test
        @DisplayName("-6 / 3 = -2")
        void divide_negative() {
            assertEquals(-2, calculator.divide(-6, 3));
        }
    }
}
