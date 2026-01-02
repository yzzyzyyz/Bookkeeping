package com.joe.accounting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService service;
    private List<Record> mockRecords;

    @BeforeEach
    void setUp() throws Exception {
        // 1. 初始化 Service
        service = new AccountService();

        // 2. 准备模拟数据 (不依赖本地文件)
        mockRecords = new ArrayList<>();
        mockRecords.add(new Record("支出", 100.0, "餐饮", LocalDate.of(2025, 1, 1), "午餐"));
        mockRecords.add(new Record("支出", 50.0, "交通", LocalDate.of(2025, 1, 5), "地铁"));
        mockRecords.add(new Record("收入", 5000.0, "工资", LocalDate.of(2025, 1, 10), "1月工资"));
        mockRecords.add(new Record("支出", 200.0, "餐饮", LocalDate.of(2025, 2, 1), "聚餐"));
        mockRecords.add(new Record("收入", 1000.0, "奖金", LocalDate.of(2025, 2, 15), "年终奖"));

        // 3. 使用反射将模拟数据注入到 Service 的 private records 字段中
        //    这样我们在测试 search 和 calculate 时，不会操作真实文件
        Field recordsField = AccountService.class.getDeclaredField("records");
        recordsField.setAccessible(true);
        recordsField.set(service, mockRecords);
    }

    // ==========================================
    // 子功能一：searchRecords (查询功能) 测试
    // 目标：覆盖所有筛选分支，测试用例 > 5条
    // ==========================================

    @Test
    @DisplayName("Search: 无任何条件，应返回所有记录")
    void testSearch_NoFilters() {
        List<Record> result = service.searchRecords(null, null, "全部", null);
        assertEquals(5, result.size(), "应返回所有 5 条记录");
    }

    @Test
    @DisplayName("Search: 按开始时间筛选")
    void testSearch_StartDate() {
        // 筛选 2025-02-01 之后的记录
        List<Record> result = service.searchRecords(LocalDate.of(2025, 2, 1), null, "全部", "");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> !r.getDate().isBefore(LocalDate.of(2025, 2, 1))));
    }

    @Test
    @DisplayName("Search: 按结束时间筛选")
    void testSearch_EndDate() {
        // 筛选 2025-01-05 之前的记录
        List<Record> result = service.searchRecords(null, LocalDate.of(2025, 1, 5), "全部", "");
        assertEquals(2, result.size()); // 1月1日和1月5日
    }

    @Test
    @DisplayName("Search: 按时间区间筛选 (边界测试)")
    void testSearch_DateRange() {
        // 筛选 1月5日 到 2月1日
        List<Record> result = service.searchRecords(LocalDate.of(2025, 1, 5), LocalDate.of(2025, 2, 1), "全部", "");
        assertEquals(3, result.size()); // 应该包含: 1.5, 1.10, 2.1
    }

    @Test
    @DisplayName("Search: 按类型筛选 - 支出")
    void testSearch_TypeExpense() {
        List<Record> result = service.searchRecords(null, null, "支出", "");
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(r -> "支出".equals(r.getType())));
    }

    @Test
    @DisplayName("Search: 按类型筛选 - 收入")
    void testSearch_TypeIncome() {
        List<Record> result = service.searchRecords(null, null, "收入", "");
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Search: 按分类名称筛选 (精确匹配)")
    void testSearch_CategoryMatch() {
        List<Record> result = service.searchRecords(null, null, "全部", "餐饮");
        assertEquals(2, result.size()); // 1.1 和 2.1 都是餐饮
    }

    @Test
    @DisplayName("Search: 按分类名称筛选 (无匹配)")
    void testSearch_CategoryNoMatch() {
        List<Record> result = service.searchRecords(null, null, "全部", "不存在的分类");
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Search: 组合条件筛选 (时间 + 类型 + 分类)")
    void testSearch_Complex() {
        // 1月, 支出, 餐饮
        List<Record> result = service.searchRecords(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31),
                "支出",
                "餐饮"
        );
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2025, 1, 1), result.get(0).getDate());
    }

    // ==========================================
    // 子功能二：统计计算 (calculateTotal / Monthly) 测试
    // 目标：覆盖累加逻辑和分组逻辑，测试用例 > 5条
    // ==========================================

    @Test
    @DisplayName("Calc: 计算总支出")
    void testCalculateTotal_Expense() {
        // 100 + 50 + 200 = 350
        double total = service.calculateTotal("支出");
        assertEquals(350.0, total, 0.001);
    }

    @Test
    @DisplayName("Calc: 计算总收入")
    void testCalculateTotal_Income() {
        // 5000 + 1000 = 6000
        double total = service.calculateTotal("收入");
        assertEquals(6000.0, total, 0.001);
    }

    @Test
    @DisplayName("Calc: 计算不存在的类型 (应为0)")
    void testCalculateTotal_UnknownType() {
        double total = service.calculateTotal("未知类型");
        assertEquals(0.0, total, 0.001);
    }

    @Test
    @DisplayName("Monthly: 按月统计支出")
    void testGetMonthlyStats_Expense() {
        Map<String, Double> stats = service.getMonthlyStats("支出");

        // 2025-01: 100 + 50 = 150
        // 2025-02: 200
        assertEquals(150.0, stats.get("2025-01"), 0.001);
        assertEquals(200.0, stats.get("2025-02"), 0.001);
        assertEquals(2, stats.size());
    }

    @Test
    @DisplayName("Monthly: 按月统计收入")
    void testGetMonthlyStats_Income() {
        Map<String, Double> stats = service.getMonthlyStats("收入");

        // 2025-01: 5000
        // 2025-02: 1000
        assertEquals(5000.0, stats.get("2025-01"), 0.001);
        assertEquals(1000.0, stats.get("2025-02"), 0.001);
    }
}