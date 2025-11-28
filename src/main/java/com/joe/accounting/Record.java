package com.joe.accounting;

import java.io.Serializable;
import java.time.LocalDate;

public class Record implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;        // 唯一标识
    private String type;      // "收入" 或 "支出"
    private double amount;    // 金额
    private String category;  // 分类 (Req002 )
    private LocalDate date;   // 时间 (Req006 )
    private String note;      // 备注

    public Record(String type, double amount, String category, LocalDate date, String note) {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    // --- Getters and Setters (手动生成或使用 Lombok @Data) ---
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }

    @Override
    public String toString() {
        return date + " [" + type + "] " + category + ": " + amount;
    }
}