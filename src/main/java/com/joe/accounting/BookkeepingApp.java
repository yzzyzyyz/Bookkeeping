package com.joe.accounting;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;

public class BookkeepingApp extends Application {
    private AccountService service = new AccountService();
    private TableView<Record> tableView = new TableView<>();
    private Label totalIncomeLabel = new Label("总收入: 0.0");
    private Label totalExpenseLabel = new Label("总支出: 0.0");

    @Override
    public void start(Stage primaryStage) {
        // --- 1. 准备两套分类数据 (Req: 支出收入分类不同) ---
        var expenseCategories = FXCollections.observableArrayList("餐饮", "交通", "购物", "娱乐", "医疗", "其他");
        var incomeCategories = FXCollections.observableArrayList("工资", "奖金", "理财", "兼职", "其他");
        // --- 1. 顶部区域：统计 + 筛选 (Req004, Req006, Req007) ---
        // 统计行
        HBox statsBox = new HBox(20, totalIncomeLabel, totalExpenseLabel);
        statsBox.setPadding(new Insets(10));
        statsBox.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 筛选行
        DatePicker startDate = new DatePicker();
        startDate.setPromptText("开始日期");
        startDate.setPrefWidth(120);

        DatePicker endDate = new DatePicker();
        endDate.setPromptText("结束日期");
        endDate.setPrefWidth(120);

        ComboBox<String> filterType = new ComboBox<>(FXCollections.observableArrayList("全部", "支出", "收入"));
        filterType.setValue("全部");
        filterType.setPrefWidth(80);

        TextField categorySearchField = new TextField();
        categorySearchField.setPromptText("输入分类(如:餐饮)");
        categorySearchField.setPrefWidth(120);

        Button searchButton = new Button("查询");
        searchButton.setOnAction(e -> {
            // Update: 调用 Service 时传入第4个参数：categorySearchField.getText()
            java.util.List<Record> result = service.searchRecords(
                    startDate.getValue(),
                    endDate.getValue(),
                    filterType.getValue(),
                    categorySearchField.getText() // <--- 获取输入的分类
            );
            tableView.setItems(FXCollections.observableArrayList(result));
        });

        Button resetButton = new Button("重置");
        resetButton.setOnAction(e -> {
            startDate.setValue(null);
            endDate.setValue(null);
            filterType.setValue("全部");
            categorySearchField.clear(); // <--- New: 清空分类输入框
            refreshTable();
        });

        HBox filterBox = new HBox(10,
                new Label("日期:"), startDate, new Label("-"), endDate,
                new Label("类型:"), filterType,
                new Label("分类:"), categorySearchField, // <--- 加在这里
                searchButton, resetButton
        );
        filterBox.setPadding(new Insets(10));
        filterBox.setStyle("-fx-background-color: #e0e0e0;");

        // 将统计和筛选放入一个垂直布局
        VBox topContainer = new VBox(statsBox, filterBox);

        // --- 2. 左侧添加表单 (Req001, Req002 [cite: 11, 13]) ---
        VBox inputBox = new VBox(10);
        inputBox.setPadding(new Insets(10));
        inputBox.setPrefWidth(250);

        // 1. 类型下拉框
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("支出", "收入"));
        typeCombo.setValue("支出");

        // 2. 分类下拉框 (默认先装入支出分类)
        ComboBox<String> categoryCombo = new ComboBox<>(expenseCategories);
        categoryCombo.setEditable(true); // 允许自定义
        categoryCombo.setValue("餐饮");

        // 3. 核心联动逻辑：当"类型"变了，"分类"列表也跟着变
        typeCombo.setOnAction(e -> {
            if ("收入".equals(typeCombo.getValue())) {
                categoryCombo.setItems(incomeCategories); // 换成收入的词库
                categoryCombo.getSelectionModel().selectFirst();
            } else {
                categoryCombo.setItems(expenseCategories); // 换成支出的词库
                categoryCombo.getSelectionModel().selectFirst();
            }
        });

        TextField amountField = new TextField();
        amountField.setPromptText("金额");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        Button addButton = new Button("添加记录");
        addButton.setMaxWidth(Double.MAX_VALUE);

        // 添加按钮逻辑
        addButton.setOnAction(e -> {
            try {
                String type = typeCombo.getValue();
                double amount = Double.parseDouble(amountField.getText());
                String category = categoryCombo.getValue();
                LocalDate date = datePicker.getValue();
                String note = noteField.getText();

                Record newRecord = new Record(type, amount, category, date, note);
                service.addRecord(newRecord);

                refreshTable(); // 刷新数据
                updateStats();  // 刷新统计

                // 清空输入框
                amountField.clear();
                noteField.clear();
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "请输入有效的金额！");
                alert.show();
            }
        });

        // 按钮1：支出图表 (修改逻辑，传入类型)
        Button expenseChartBtn = new Button("支出分布图");
        expenseChartBtn.setMaxWidth(Double.MAX_VALUE);
        expenseChartBtn.setOnAction(e -> showPieChartWindow("支出")); // 传入 "支出"

        // 按钮2：收入图表 (新功能)
        Button incomeChartBtn = new Button("收入分布图");
        incomeChartBtn.setMaxWidth(Double.MAX_VALUE);
        incomeChartBtn.setOnAction(e -> showPieChartWindow("收入")); // 传入 "收入"

        // 按钮3：月度统计详情 (新功能)
        Button monthlyStatsBtn = new Button("查看月度收支表");
        monthlyStatsBtn.setMaxWidth(Double.MAX_VALUE);
        monthlyStatsBtn.setOnAction(e -> showMonthlyStatsWindow());

        // 将所有按钮加入布局
        inputBox.getChildren().addAll(
                new Label("类型:"), typeCombo,
                new Label("金额:"), amountField,
                new Label("分类:"), categoryCombo,
                new Label("日期:"), datePicker,
                new Label("备注:"), noteField,
                new Region(),
                addButton,
                expenseChartBtn, // Update
                incomeChartBtn,  // New
                monthlyStatsBtn  // New
        );

        // --- 3. 中间数据列表 (Req008 [cite: 64]) ---
        TableColumn<Record, String> dateCol = new TableColumn<>("日期");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Record, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Record, String> catCol = new TableColumn<>("分类");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Record, Double> amountCol = new TableColumn<>("金额");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Record, String> noteCol = new TableColumn<>("备注");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));

        tableView.getColumns().addAll(dateCol, typeCol, catCol, amountCol, noteCol);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("删除此记录");
        deleteItem.setOnAction(e -> {
            Record selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                service.deleteRecord(selected); // 调用 Service 删除
                refreshTable(); // 刷新表格
                updateStats();  // 刷新总金额
            }
        });
        contextMenu.getItems().add(deleteItem);
        tableView.setContextMenu(contextMenu);

        // --- 4. 组装界面 ---
        BorderPane root = new BorderPane();
        root.setTop(topContainer); // 以前是 root.setTop(statsBox);
        // 创建一个滚动面板包裹 inputBox
        ScrollPane scrollPane = new ScrollPane(inputBox);
        scrollPane.setFitToWidth(true); // 让内容宽度自适应
        scrollPane.setStyle("-fx-background-color:transparent;"); // 去掉边框背景

        // 放到左边
        root.setLeft(scrollPane);
        root.setCenter(tableView);

        // 初始化数据
        refreshTable();
        updateStats();

        Scene scene = new Scene(root, 800, 500);
        primaryStage.setTitle("个人记账本系统 v1.0");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void refreshTable() {
        tableView.setItems(FXCollections.observableArrayList(service.getAllRecords()));
    }

    private void updateStats() {
        double income = service.calculateTotal("收入");
        double expense = service.calculateTotal("支出");
        totalIncomeLabel.setText("总收入: " + String.format("%.2f", income));
        totalExpenseLabel.setText("总支出: " + String.format("%.2f", expense));
        // 可以简单根据收入-支出改变颜色
    }

    // 修改：支持传入类型 (type) 来生成不同的饼图
    private void showPieChartWindow(String type) {
        Stage chartStage = new Stage();
        chartStage.setTitle(type + "分类统计");

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        java.util.Map<String, Double> summary = new java.util.HashMap<>();

        // 只统计指定类型 (收入 或 支出)
        for (Record r : service.getAllRecords()) {
            if (type.equals(r.getType())) {
                summary.put(r.getCategory(), summary.getOrDefault(r.getCategory(), 0.0) + r.getAmount());
            }
        }
        summary.forEach((cat, amt) -> pieData.add(new PieChart.Data(cat, amt)));

        PieChart chart = new PieChart(pieData);
        chart.setTitle(type + "构成分析");

        Scene scene = new Scene(new BorderPane(chart), 600, 400);
        chartStage.setScene(scene);
        chartStage.show();
    }

    // 新增：显示月度统计窗口
    private void showMonthlyStatsWindow() {
        Stage stage = new Stage();
        stage.setTitle("月度收支统计表");

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: 'monospaced'; -fx-font-size: 14px;");

        StringBuilder sb = new StringBuilder();
        sb.append("========== 月度支出 ==========\n");
        // 获取支出统计
        service.getMonthlyStats("支出").forEach((month, amount) ->
                sb.append(String.format("%s :  -%.2f\n", month, amount))
        );

        sb.append("\n========== 月度收入 ==========\n");
        // 获取收入统计
        service.getMonthlyStats("收入").forEach((month, amount) ->
                sb.append(String.format("%s :  +%.2f\n", month, amount))
        );

        textArea.setText(sb.toString());

        Scene scene = new Scene(new BorderPane(textArea), 400, 500);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}