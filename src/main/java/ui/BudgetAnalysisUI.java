package ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.BudgetData;
import service.BudgetService;

import java.util.Map;
import javafx.scene.control.Tooltip;

public class BudgetAnalysisUI {

    private final Scene scene;

    // Soft Modern Palette
    private final String BLUE = "#64B5F6";
    private final String ORANGE = "#FFB74D";
    private final String PURPLE = "#BA68C8";
    private final String GREEN = "#66BB6A";

    public BudgetAnalysisUI() {

        BudgetService service = new BudgetService();
        BudgetData data = service.getBudgetAnalysis();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F7FA;");
        root.setPadding(new Insets(30));

        VBox topSection = new VBox(20);
        topSection.getChildren().addAll(createSummaryCards(data));
        root.setTop(topSection);

        // ================= CHART SECTION =================
        HBox chartSection = new HBox(40);
        chartSection.setAlignment(Pos.CENTER);
        chartSection.setPadding(new Insets(20, 0, 0, 0));
        
        chartSection.setFillHeight(true);
        HBox.setHgrow(chartSection, Priority.ALWAYS);

        chartSection.getChildren().addAll(
                createDonutCard(data),
                createBarCard(data)
        );

        root.setCenter(chartSection);

        scene = new Scene(root, 1200, 750);
    }

    // ================= SUMMARY CARDS =================

    private HBox createSummaryCards(BudgetData data) {

        HBox box = new HBox(30);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(1100);

        box.getChildren().addAll(
                createPremiumCard("Total Budget", "₹2,50,000", "#6A85B6, #B8C6DB"),
                createPremiumCard("Amount Spent", "₹1,50,000", "#FF9A9E, #FAD0C4"),
                createPremiumCard("Remaining Balance", "₹1,00,000", "#56ab2f, #a8e063")
        );

        return box;
    }

    private VBox createPremiumCard(String title, String amount, String gradient) {

    Label titleLbl = new Label(title);
    titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18;");

    Label amountLbl = new Label(amount);
    amountLbl.setStyle("-fx-text-fill: white; -fx-font-size: 28; -fx-font-weight: bold;");

    VBox topSection = new VBox(10, titleLbl, amountLbl);
    topSection.setPadding(new Insets(20));
    topSection.setStyle(
            "-fx-background-radius: 20 20 0 0;" +
            "-fx-background-color: linear-gradient(to right, " + gradient + ");"
    );

    Label bottomAmount = new Label(amount);
    bottomAmount.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #333;");

    VBox bottomSection = new VBox(bottomAmount);
    bottomSection.setPadding(new Insets(15));
    bottomSection.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 0 0 20 20;"
    );

    VBox card = new VBox(topSection, bottomSection);
    card.setPrefWidth(300);
    card.setStyle(
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 25, 0, 0, 8);"
    );

    return card;
}

    // ================= DONUT CHART =================

    private VBox createDonutCard(BudgetData data) {

        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(false);

        for (Map.Entry<String, Double> entry :
                data.getCategoryDistribution().entrySet()) {
            pieChart.getData().add(
                    new PieChart.Data(entry.getKey(), entry.getValue())
            );
        }

        // Apply pastel colors
        Platform.runLater(() -> {
            pieChart.getData().get(0).getNode().setStyle("-fx-pie-color:" + ORANGE + ";");
            pieChart.getData().get(1).getNode().setStyle("-fx-pie-color:" + BLUE + ";");
            pieChart.getData().get(2).getNode().setStyle("-fx-pie-color:" + PURPLE + ";");
            pieChart.getData().get(3).getNode().setStyle("-fx-pie-color:" + GREEN + ";");
        });

        pieChart.setPrefSize(280, 280);
        pieChart.setMinSize(280,280);
        pieChart.setMaxSize(280,280);

        Circle hole = new Circle(80);
        hole.setFill(Color.WHITE);
        Label center = new Label("₹ " + data.getTotalBudget() + "\nTotal Stock");
        center.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        center.setAlignment(Pos.CENTER);
        center.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Tooltip tooltip = new Tooltip("Total Budget");
        Tooltip.install(center, tooltip);
        
        StackPane donut = new StackPane(pieChart, hole, center);

        VBox legend = new VBox(12);

        legend.getChildren().addAll(
                createLegendItem(ORANGE, "Furniture", "₹80,000"),
                createLegendItem(BLUE, "Stationery", "₹60,000"),
                createLegendItem(PURPLE, "Miscellaneous", "₹50,000"),
                createLegendItem(GREEN, "Computer", "₹60,000")
        );

        VBox layout = new VBox(20, donut, legend);
        layout.setAlignment(Pos.CENTER);

        VBox card = wrapInCard(layout);

        card.setPrefWidth(420);
        card.setMaxWidth(420);
        
        return card;
    }
    private HBox createLegendItem(String color, String name, String amount){

        Circle circle = new Circle(7, Color.web(color));

        Label label = new Label(name);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        Label value = new Label(amount);
        value.setFont(Font.font("Segoe UI", 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(10, circle, label, spacer, value);
    }

    private void showBarValues(XYChart.Series<String, Number> series){

        for (XYChart.Data<String, Number> data : series.getData()) {

            Label label = new Label(String.valueOf(data.getYValue()));

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {

                if (newNode != null) {

                    StackPane parent = (StackPane) newNode;
                    parent.getChildren().add(label);

                    StackPane.setAlignment(label, Pos.TOP_CENTER);
                    label.setTranslateY(-10);
                }

            });
        }
    }
    // ================= BAR CHART =================

    private VBox createBarCard(BudgetData data) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setTickLabelFill(Color.web("#555"));
        yAxis.setTickLabelFill(Color.web("#555"));

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(30);
        barChart.setBarGap(10);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (Map.Entry<String, Double> entry :
                data.getCategoryDistribution().entrySet()) {
            series.getData().add(
                    new XYChart.Data<>(entry.getKey(), entry.getValue())
            );
        }

        barChart.getData().add(series);
        showBarValues(series);

        // Color bars individually
        Platform.runLater(() -> {
            series.getData().get(0).getNode().setStyle("-fx-bar-fill:" + ORANGE + ";");
            series.getData().get(1).getNode().setStyle("-fx-bar-fill:" + BLUE + ";");
            series.getData().get(2).getNode().setStyle("-fx-bar-fill:" + PURPLE + ";");
            series.getData().get(3).getNode().setStyle("-fx-bar-fill:" + GREEN + ";");
        });

        barChart.lookup(".chart-plot-background")
                .setStyle("-fx-background-color: transparent;");

        Label label = new Label("Product-wise Overview");
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#444"));

        VBox layout = new VBox(15, barChart, label);
        layout.setAlignment(Pos.CENTER);

        VBox card = wrapInCard(layout);

        card.setPrefWidth(420);
        card.setMaxWidth(420);

        return card;
    }

    // ================= CARD WRAPPER =================

    private VBox wrapInCard(javafx.scene.Node node) {

        VBox card = new VBox(node);
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 25;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 25, 0, 0, 8);
                """);

        return card;
    }

    public Scene getScene() {
        return scene;
    }
}