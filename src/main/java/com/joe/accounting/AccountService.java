package com.joe.accounting;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccountService {
    private static final String DATA_FILE = "account_data.dat";
    private List<Record> records;

    public AccountService() {
        this.records = loadRecords();
    }

    // 添加记录 (Req001 )
    public void addRecord(Record record) {
        records.add(record);
        saveRecords();
    }

    // 获取所有记录
    public List<Record> getAllRecords() {
        return records;
    }

    public void deleteRecord(Record record) {
        records.remove(record);
        saveRecords(); // 删除后立即保存文件
    }

    // 筛选记录 (Req006, Req007 [cite: 28, 31])
    public List<Record> searchRecords(LocalDate start, LocalDate end, String type, String categoryQuery) {
        return records.stream()
                // 1. 时间筛选
                .filter(r -> (start == null || !r.getDate().isBefore(start)))
                .filter(r -> (end == null || !r.getDate().isAfter(end)))
                // 2. 类型筛选 (收入/支出/全部)
                .filter(r -> (type == null || "全部".equals(type) || r.getType().equals(type)))
                // 3. 新增：分类筛选 (如果用户输入了内容，则必须匹配分类名称)
                .filter(r -> (categoryQuery == null || categoryQuery.trim().isEmpty() || r.getCategory().equals(categoryQuery.trim())))
                .collect(Collectors.toList());
    }

    // 统计总收入/支出 (Req004 )
    public double calculateTotal(String type) {
        return records.stream()
                .filter(r -> r.getType().equals(type))
                .mapToDouble(Record::getAmount)
                .sum();
    }

    // 新增：获取月度统计数据 (返回格式如：{"2025-11": 5000.0, "2025-12": 6000.0})
    public java.util.Map<String, Double> getMonthlyStats(String type) {
        // 使用 TreeMap 让月份自动按时间排序
        java.util.Map<String, Double> stats = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");

        for (Record r : records) {
            if (r.getType().equals(type)) {
                String month = r.getDate().format(formatter);
                stats.put(month, stats.getOrDefault(month, 0.0) + r.getAmount());
            }
        }
        return stats;
    }

    // 新增：更新记录 (Req003)
    public void updateRecord(Record oldRecord, Record newRecord) {
        int index = records.indexOf(oldRecord);
        if (index != -1) {
            records.set(index, newRecord); // 替换旧记录
            saveRecords(); // 保存文件
        }
    }

    // 本地存储实现 (DataStorage )
    private void saveRecords() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(records);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Record> loadRecords() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Record>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}