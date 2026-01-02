package com.joe.accounting;

import com.code_intelligence.jazzer.Jazzer;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.time.LocalDate;
import java.util.List;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class FuzzSearchTest {

    private static AccountService service;

    // --- 修复点：添加 throws Exception ---
    // 允许 main 方法向上抛出 Jazzer 可能产生的异常
    public static void main(String[] args) throws Exception {
        Jazzer.main(args);
    }

    public static void fuzzerInitialize() {
        service = new AccountService();
        // 初始化模拟数据
        List<Record> mockRecords = new ArrayList<>();
        mockRecords.add(new Record("支出", 100.0, "餐饮", LocalDate.of(2025, 1, 1), "测试1"));
        mockRecords.add(new Record("收入", 5000.0, "工资", LocalDate.of(2025, 2, 1), "测试2"));
        try {
            Field recordsField = AccountService.class.getDeclaredField("records");
            recordsField.setAccessible(true);
            recordsField.set(service, mockRecords);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LocalDate start = null;
        if (data.consumeBoolean()) start = LocalDate.ofEpochDay(data.consumeInt(0, 20000));

        LocalDate end = null;
        if (data.consumeBoolean()) end = LocalDate.ofEpochDay(data.consumeInt(0, 20000));

        String type = data.consumeBoolean() ? data.pickValue(new String[]{"收入", "支出", "全部"}) : data.consumeString(10);
        String categoryQuery = data.consumeString(20);

        try {
            service.searchRecords(start, end, type, categoryQuery);
        } catch (Exception e) {
            // 显式捕获我们埋下的“逻辑炸弹”，抛出以触发 Crash
            if (e instanceof IllegalStateException) {
                throw e;
            }
            // 忽略其他非崩溃性异常
            if (e instanceof NullPointerException || e instanceof ArrayIndexOutOfBoundsException) {
                throw e;
            }
        }
    }
}