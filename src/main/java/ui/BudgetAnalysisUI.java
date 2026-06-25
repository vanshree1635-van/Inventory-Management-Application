package ui;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.BudgetData;
import service.BudgetService;

import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

public class BudgetAnalysisUI {

    private final Scene      scene;
    private final BorderPane root;  // field so snapshot captures full layout

    // ── Colour palette ───────────────────────────────────────────────────────
    private final String ORANGE   = "#FFB74D";
    private final String BLUE     = "#64B5F6";
    private final String PURPLE   = "#BA68C8";
    private final String GREEN    = "#66BB6A";

    // Pastel variants for "previous month" bars
    private final String ORANGE_P = "#FFE0A3";
    private final String BLUE_P   = "#BBDEFB";
    private final String PURPLE_P = "#E1BEE7";
    private final String GREEN_P  = "#C8E6C9";

    private final String CURR_LABEL;
    private final String PREV_LABEL;

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String fmt(double v) { return String.format("%.2f", v); }

    private String pctChange(double curr, double prev) {
        if (prev == 0) return curr == 0 ? "—" : "+∞%";
        double pct = ((curr - prev) / prev) * 100.0;
        return String.format("%+.1f%%", pct);
    }

    // ── Constructor ───────────────────────────────────────────────────────────
    public BudgetAnalysisUI() {

        LocalDate now       = LocalDate.now();
        LocalDate prevMonth = now.minusMonths(1);

        CURR_LABEL = now.getMonth()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + now.getYear();
        PREV_LABEL = prevMonth.getMonth()
                              .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + prevMonth.getYear();

        BudgetService service = new BudgetService();
        BudgetData    data    = service.getBudgetAnalysis();

        root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F7FA;");
        root.setPadding(new Insets(30));

        // TOP: heading + summary cards
        VBox topSection = new VBox(14);
        topSection.getChildren().addAll(
            createPageHeader(),
            createSummaryCards(data)
        );
        root.setTop(topSection);

        // CENTER: donut + grouped bar chart
        HBox chartSection = new HBox(40);
        chartSection.setAlignment(Pos.CENTER);
        chartSection.setPadding(new Insets(20, 0, 0, 0));
        HBox.setHgrow(chartSection, Priority.ALWAYS);
        chartSection.getChildren().addAll(
            createDonutCard(data),
            createGroupedBarCard(data)
        );
        root.setCenter(chartSection);

        // BOTTOM: Save as JPG button
        root.setBottom(createBottomBar());

        scene = new Scene(root, 1200, 800);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PAGE HEADER
    // ═════════════════════════════════════════════════════════════════════════

    private HBox createPageHeader() {

        Label title = new Label("Budget Comparison");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#333"));

        HBox legendBox = new HBox(20,
            makeLegendDot("#555555", CURR_LABEL + " (Current)"),
            makeLegendDot("#BBBBBB", PREV_LABEL + " (Previous)")
        );
        legendBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(title, spacer, legendBox);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox makeLegendDot(String color, String text) {

        Rectangle sq = new Rectangle(14, 14);
        sq.setArcWidth(4);
        sq.setArcHeight(4);
        sq.setFill(Color.web(color));

        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 13));
        lbl.setTextFill(Color.web("#555"));

        HBox h = new HBox(6, sq, lbl);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BOTTOM BAR — Save as JPG
    // ═════════════════════════════════════════════════════════════════════════

    private HBox createBottomBar() {

        Button saveBtn = new Button("⬇  Save as JPG");
        saveBtn.setStyle(
            "-fx-background-color: #5C85D6;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 8 22 8 22;" +
            "-fx-cursor: hand;"
        );
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(
            "-fx-background-color: #4A72C4;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 8 22 8 22;" +
            "-fx-cursor: hand;"
        ));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle(
            "-fx-background-color: #5C85D6;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 8 22 8 22;" +
            "-fx-cursor: hand;"
        ));

        saveBtn.setOnAction(e -> saveSceneAsJpg());

        HBox bar = new HBox(saveBtn);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(18, 0, 0, 0));
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SNAPSHOT → JPG  (same logic as ReportUI.saveAsJpg)
    // ═════════════════════════════════════════════════════════════════════════

    private void saveSceneAsJpg() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Budget Report as JPG");
        chooser.setInitialFileName(
            "budget_" + CURR_LABEL.replace(" ", "_")
            + "_vs_" + PREV_LABEL.replace(" ", "_") + ".jpg");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JPEG Image (*.jpg)", "*.jpg"));

        Stage stage = (Stage) scene.getWindow();
        File  file  = chooser.showSaveDialog(stage);
        if (file == null) return;   // user cancelled

        try {
            // 1. Snapshot the full root pane
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.web("#F5F7FA"));
            WritableImage fxImage = root.snapshot(params, null);

            // 2. Convert WritableImage → BufferedImage
            java.awt.image.BufferedImage buffered =
                SwingFXUtils.fromFXImage(fxImage, null);

            // 3. Strip alpha → pure RGB  (JPG does NOT support alpha channel)
            java.awt.image.BufferedImage rgb =
                new java.awt.image.BufferedImage(
                    buffered.getWidth(),
                    buffered.getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_RGB
                );
            java.awt.Graphics2D g = rgb.createGraphics();
            g.drawImage(buffered, 0, 0, java.awt.Color.WHITE, null);
            g.dispose();

            // 4. Write JPEG to disk
            ImageIO.write(rgb, "jpg", file);

            // 5. Confirmation alert (same style as ReportUI)
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Saved");
            alert.setHeaderText(null);
            alert.setContentText("Report saved as JPG:\n" + file.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Could not save image:\n" + ex.getMessage());
            alert.showAndWait();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SUMMARY CARDS  (3 comparison cards)
    // ═════════════════════════════════════════════════════════════════════════

    private HBox createSummaryCards(BudgetData data) {

        HBox box = new HBox(30);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(1100);

        box.getChildren().addAll(
            createComparisonCard(
                "Total Budget",
                data.getTotalBudget(),
                data.getPrevTotalBudget(),
                "#6A85B6, #B8C6DB"),
            createComparisonCard(
                "Amount Spent",
                data.getAmountSpent(),
                data.getPrevAmountSpent(),
                "#FF9A9E, #FAD0C4"),
            createComparisonCard(
                "Remaining Balance",
                data.getRemainingBalance(),
                data.getPrevRemainingBalance(),
                "#56ab2f, #a8e063")
        );

        return box;
    }

    /**
     * Gradient header  → current month value (large)
     * White footer     → previous month value + ▲/▼ % change
     */
    private VBox createComparisonCard(String title,
                                      double curr,
                                      double prev,
                                      String gradient) {

        // Gradient header
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16;");

        Label monthLbl = new Label(CURR_LABEL);
        monthLbl.setStyle(
            "-fx-text-fill: rgba(255,255,255,0.80); -fx-font-size: 12;");

        Label amountLbl = new Label("₹" + fmt(curr));
        amountLbl.setStyle(
            "-fx-text-fill: white; -fx-font-size: 26; -fx-font-weight: bold;");

        VBox header = new VBox(4, titleLbl, monthLbl, amountLbl);
        header.setPadding(new Insets(20));
        header.setStyle(
            "-fx-background-radius: 20 20 0 0;" +
            "-fx-background-color: linear-gradient(to right, " + gradient + ");");

        // White footer
        Label prevLbl = new Label(PREV_LABEL + ":  ₹" + fmt(prev));
        prevLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #666;");

        String  pct = pctChange(curr, prev);
        boolean up  = curr >= prev;
        Label deltaLbl = new Label((up ? "▲ " : "▼ ") + pct);
        deltaLbl.setStyle(
            "-fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-text-fill: " + (up ? "#2E7D32" : "#C62828") + ";");

        HBox footer = new HBox(10, prevLbl, deltaLbl);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(14, 20, 14, 20));
        footer.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 0 0 20 20;");

        // Card shell
        VBox card = new VBox(header, footer);
        card.setPrefWidth(300);
        card.setStyle(
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 25, 0, 0, 8);");
        return card;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DONUT CHART  (current month distribution)
    // ═════════════════════════════════════════════════════════════════════════

    private VBox createDonutCard(BudgetData data) {

        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(false);

        List<String> categories =
            new ArrayList<>(data.getCategoryDistribution().keySet());

        for (String cat : categories)
            pieChart.getData().add(
                new PieChart.Data(cat,
                    data.getCategoryDistribution().get(cat)));

        String[] solidColors = { ORANGE, BLUE, PURPLE, GREEN };

        Platform.runLater(() -> {
            for (int i = 0;
                 i < pieChart.getData().size() && i < solidColors.length;
                 i++) {
                pieChart.getData().get(i).getNode()
                    .setStyle("-fx-pie-color:" + solidColors[i] + ";");
            }
        });

        pieChart.setPrefSize(280, 280);
        pieChart.setMinSize(280, 280);
        pieChart.setMaxSize(280, 280);

        // Donut hole overlay
        Circle hole = new Circle(80, Color.WHITE);
        Label center = new Label("₹" + fmt(data.getTotalBudget())
                                 + "\n" + CURR_LABEL);
        center.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        center.setAlignment(Pos.CENTER);
        center.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Tooltip.install(center, new Tooltip("Current Month — Total Budget"));

        StackPane donut = new StackPane(pieChart, hole, center);

        // Per-category legend with delta arrows
        String[] catColors  = { ORANGE,   BLUE,   PURPLE,   GREEN   };
        String[] catColorsP = { ORANGE_P, BLUE_P, PURPLE_P, GREEN_P };

        VBox legend = new VBox(10);
        for (int i = 0; i < categories.size() && i < catColors.length; i++) {
            String cat     = categories.get(i);
            double currVal = data.getCategoryDistribution()
                                 .getOrDefault(cat, 0.0);
            double prevVal = data.getPrevCategoryDistribution()
                                 .getOrDefault(cat, 0.0);
            legend.getChildren().add(
                createLegendItem(catColors[i], catColorsP[i],
                                 cat, currVal, prevVal));
        }

        Label heading = new Label("Current Month Distribution");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        heading.setTextFill(Color.web("#444"));

        VBox layout = new VBox(14, heading, donut, legend);
        layout.setAlignment(Pos.CENTER);

        VBox card = wrapInCard(layout);
        card.setPrefWidth(430);
        card.setMaxWidth(430);
        return card;
    }

    /** dot · name · spacer · ₹value · ▲/▼% */
    private HBox createLegendItem(String colorCurr, String colorPrev,
                                  String name,
                                  double curr, double prev) {

        Circle dot = new Circle(7, Color.web(colorCurr));

        Label nameLbl = new Label(name);
        nameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        Label currLbl = new Label("₹" + fmt(curr));
        currLbl.setFont(Font.font("Segoe UI", 13));
        currLbl.setTextFill(Color.web("#333"));

        String  pct = pctChange(curr, prev);
        boolean up  = curr >= prev;
        Label deltaLbl = new Label((up ? "▲ " : "▼ ") + pct);
        deltaLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        deltaLbl.setTextFill(Color.web(up ? "#2E7D32" : "#C62828"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(8, dot, nameLbl, spacer, currLbl, deltaLbl);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GROUPED BAR CHART  (previous vs current, per category)
    // ═════════════════════════════════════════════════════════════════════════

    private VBox createGroupedBarCard(BudgetData data) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setTickLabelFill(Color.web("#555"));
        yAxis.setTickLabelFill(Color.web("#555"));

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(25);
        barChart.setBarGap(4);
        barChart.setPrefHeight(320);

        // Union of categories from both months
        Set<String> allCats = new LinkedHashSet<>();
        allCats.addAll(data.getCategoryDistribution().keySet());
        allCats.addAll(data.getPrevCategoryDistribution().keySet());

        XYChart.Series<String, Number> currSeries = new XYChart.Series<>();
        currSeries.setName(CURR_LABEL);

        XYChart.Series<String, Number> prevSeries = new XYChart.Series<>();
        prevSeries.setName(PREV_LABEL);

        for (String cat : allCats) {
            currSeries.getData().add(new XYChart.Data<>(cat,
                data.getCategoryDistribution().getOrDefault(cat, 0.0)));
            prevSeries.getData().add(new XYChart.Data<>(cat,
                data.getPrevCategoryDistribution().getOrDefault(cat, 0.0)));
        }

        barChart.getData().addAll(currSeries, prevSeries);
        addBarLabels(currSeries);
        addBarLabels(prevSeries);

        // current = solid colours, previous = pastel
        String[] solidColors  = { ORANGE,   BLUE,   PURPLE,   GREEN   };
        String[] pastelColors = { ORANGE_P, BLUE_P, PURPLE_P, GREEN_P };

        Platform.runLater(() -> {

            int i = 0;
            for (XYChart.Data<String, Number> d : currSeries.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle(
                        "-fx-bar-fill:"
                        + solidColors[i % solidColors.length] + ";");
                i++;
            }

            int j = 0;
            for (XYChart.Data<String, Number> d : prevSeries.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle(
                        "-fx-bar-fill:"
                        + pastelColors[j % pastelColors.length] + ";");
                j++;
            }

            var bg = barChart.lookup(".chart-plot-background");
            if (bg != null) bg.setStyle("-fx-background-color: transparent;");
        });

        HBox legendBox = new HBox(20,
            makeLegendDot("#888888", CURR_LABEL + " (Current)"),
            makeLegendDot("#CCCCCC", PREV_LABEL + " (Previous)")
        );
        legendBox.setAlignment(Pos.CENTER);

        Label heading = new Label(
            "Category-wise Comparison  ("
            + PREV_LABEL + "  vs  " + CURR_LABEL + ")");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        heading.setTextFill(Color.web("#444"));

        VBox layout = new VBox(12, heading, barChart, legendBox);
        layout.setAlignment(Pos.CENTER);

        VBox card = wrapInCard(layout);
        card.setPrefWidth(560);
        card.setMaxWidth(560);
        return card;
    }

    /** Small value label floating above each bar. */
    private void addBarLabels(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> d : series.getData()) {
            Label lbl = new Label(
                String.format("%.0f", d.getYValue().doubleValue()));
            lbl.setFont(Font.font("Segoe UI", 10));
            lbl.setTextFill(Color.web("#444"));
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode instanceof StackPane sp) {
                    sp.getChildren().add(lbl);
                    StackPane.setAlignment(lbl, Pos.TOP_CENTER);
                    lbl.setTranslateY(-14);
                }
            });
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CARD WRAPPER
    // ═════════════════════════════════════════════════════════════════════════

    private VBox wrapInCard(javafx.scene.Node node) {
        VBox card = new VBox(node);
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 25;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 25, 0, 0, 8);"
        );
        return card;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC ACCESSOR
    // ═════════════════════════════════════════════════════════════════════════

    public Scene getScene() { return scene; }
}
