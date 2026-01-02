package com.joe.accounting;

import org.junit.jupiter.api.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试：AccountService + 文件系统
 * 策略：自底向上 (Bottom-Up)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    private static final String DATA_FILE = "account_data.dat";
    private static final String BACKUP_FILE = "account_data.dat.bak";

    // --- 环境准备与清理 (Fixture) ---

    @BeforeAll
    static void backupRealData() {
        // 1. 如果存在真实的 account_data.dat，先改名为 .bak 备份，避免测试污染真实数据
        File file = new File(DATA_FILE);
        if (file.exists()) {
            file.renameTo(new File(BACKUP_FILE));
            System.out.println("Integration Test: 已备份真实数据文件。");
        }
    }

    @AfterAll
    static void restoreRealData() {
        // 2. 测试结束后，删除测试生成的垃圾文件，并恢复真实数据
        File file = new File(DATA_FILE);
        if (file.exists()) {
            file.delete(); // 删除测试数据
        }

        File backup = new File(BACKUP_FILE);
        if (backup.exists()) {
            backup.renameTo(new File(DATA_FILE));
            System.out.println("Integration Test: 已恢复真实数据文件。");
        }
    }

    @BeforeEach
    void cleanTestData() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            // 尝试删除文件
            boolean deleted = file.delete();
            // 如果删除失败（常见于 Windows 文件占用），尝试强制回收资源并重试
            if (!deleted) {
                System.gc(); // 提示 JVM 回收未关闭的文件流
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                file.delete(); // 再次尝试删除
            }
        }
    }

    // --- 第 1 组：持久化集成测试 (Service <-> File System) ---
    // 验证数据能否真正写入磁盘，并在程序重启后读取回来
    @Test
    @Order(1)
    @DisplayName("Integration Group 1: 数据持久化与重载测试")
    void testPersistenceIntegration() {
        // 步骤 1: 启动第一个 Service 实例 (Session A)
        AccountService serviceA = new AccountService();
        Record record = new Record("支出", 100.0, "测试分类", LocalDate.now(), "持久化测试");
        serviceA.addRecord(record);

        // 验证文件是否已创建
        File file = new File(DATA_FILE);
        assertTrue(file.exists(), "数据文件应当被创建");
        assertTrue(file.length() > 0, "数据文件不应为空");

        // 步骤 2: 模拟程序重启 (创建全新的 Service 实例 Session B)
        // 注意：这里不传入任何 Mock 数据，完全依赖文件读取
        AccountService serviceB = new AccountService();
        List<Record> loadedRecords = serviceB.getAllRecords();

        // 步骤 3: 验证数据一致性
        assertEquals(1, loadedRecords.size(), "重启后应能读取到 1 条记录");
        Record loadedRecord = loadedRecords.get(0);
        assertEquals("测试分类", loadedRecord.getCategory());
        assertEquals(100.0, loadedRecord.getAmount(), 0.001);
        assertEquals("持久化测试", loadedRecord.getNote());
    }

    // --- 第 2 组：业务流集成测试 (Workflow Integration) ---
    // 模拟用户完整操作：记账 -> 改错 -> 查月账 -> 删账
    @Test
    @Order(2)
    @DisplayName("Integration Group 2: 完整业务流程集成 (Add->Update->Stats->Delete)")
    void testWorkflowIntegration() {
        AccountService service = new AccountService();

        // 【新增防卫性逻辑】
        // 检查是否受上一个测试影响残留了数据
        if (!service.getAllRecords().isEmpty()) {
            System.out.println("Warning: Found residual data (Isolation failure), forcing cleanup...");
            // 强制删除物理文件
            new File(DATA_FILE).delete();
            // 重新初始化 Service 以确保加载的是空状态
            service = new AccountService();
        }

        // --- 以下是正常的业务测试流程 ---

        // 1. [Add] 用户添加两条记录
        Record r1 = new Record("支出", 200.0, "餐饮", LocalDate.of(2025, 5, 1), "午饭");
        Record r2 = new Record("收入", 5000.0, "工资", LocalDate.of(2025, 5, 15), "五月工资");
        service.addRecord(r1);
        service.addRecord(r2);

        // 2. [Update] 用户发现午饭金额记错了，从 200 改为 250
        Record r1_updated = new Record("支出", 250.0, "餐饮", LocalDate.of(2025, 5, 1), "午饭(大餐)");
        service.updateRecord(r1, r1_updated);

        // 验证修改结果 (Service内存数据)
        List<Record> all = service.getAllRecords();
        // 这里 all.get(0) 应该是我们修改后的记录，金额 250.0
        assertEquals(250.0, all.get(0).getAmount(), 0.001, "更新后金额应为 250.0");
        assertEquals("午饭(大餐)", all.get(0).getNote());

        // 3. [Stats] 用户查看 2025-05 的月度报表
        // 验证 Add 和 Update 对统计模块的影响
        Map<String, Double> expenseStats = service.getMonthlyStats("支出");
        assertEquals(250.0, expenseStats.get("2025-05"), 0.001, "月度支出统计应包含修改后的金额");

        Map<String, Double> incomeStats = service.getMonthlyStats("收入");
        assertEquals(5000.0, incomeStats.get("2025-05"), 0.001);

        // 4. [Delete] 用户删除收入记录
        // 注意：由于 updateRecord 实际上替换了对象引用，我们需要确保删除的是当前列表中的对象
        // 但在这个简单的实现中，deleteRecord 使用 equals (或对象引用) 来删除。
        // r2 对象自从 add 之后没变过，所以可以直接删。
        service.deleteRecord(r2);

        // 验证删除
        assertEquals(1, service.getAllRecords().size(), "删除后应只剩 1 条记录");
        assertEquals("餐饮", service.getAllRecords().get(0).getCategory());

        // 再次验证统计
        assertEquals(0, service.calculateTotal("收入"), 0.001, "删除后总收入应为 0");
    }
}