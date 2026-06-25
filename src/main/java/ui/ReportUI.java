package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.geometry.Side;
import javafx.scene.layout.StackPane;




public class ReportUI {

    private final Stage stage;
    private VBox dashboard;

    private final String[] categories = {
        "Furniture",
        "Stationery",
        "Computer",
        "Miscellaneous"
    };



    public ReportUI(String period) {
        stage = new Stage();
        stage.setTitle(period + " Report");

  Label title = new Label(period + " Inventory Report");
title.setStyle(
    "-fx-font-size: 26px;" +
    "-fx-font-weight: bold;" +
    "-fx-text-fill: #2c3e50;"
);


        Label purchase = new Label();
Label issued = new Label();
Label returned = new Label();
Label pending = new Label();


        HBox cards = new HBox(20,
        createCard("Total Purchase", purchase, "#1abc9c"),
        createCard("Total Issued", issued, "#3498db"),
        createCard("Total Returned", returned, "#9b59b6"),
        createCard("Pending Stock", pending, "#e67e22")
);
cards.setAlignment(Pos.CENTER);

dashboard = new VBox(20);
dashboard.setAlignment(Pos.CENTER);

VBox root = new VBox(35, title, cards, dashboard);
root.setPadding(new Insets(40));
root.setStyle(
    "-fx-background-color: linear-gradient(to bottom right, #f8f9fc, #eef1f7);"
);



        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        Map<String, model.ReportData> data = loadReport(period);

int p = 0, i = 0, r = 0;

for (model.ReportData d : data.values()) {
    p += d.purchase;
    i += d.issued;
    r += d.returned;
}

purchase.setText(String.valueOf(p));
issued.setText(String.valueOf(i));
returned.setText(String.valueOf(r));
pending.setText(String.valueOf(p - i + r));

/*issued.setText("Total Issued: " + i);
returned.setText("Total Returned: " + r);
pending.setText("Pending Stock: " + (p - i + r));
*/
HBox charts = new HBox(60,
    createBarChart(data),
    createCategoryPie()
);
charts.setAlignment(Pos.CENTER);
charts.setPadding(new Insets(20, 0, 0, 0));

dashboard.getChildren().add(charts);


stage.setScene(new Scene(root, 900, 700));

    }

    public void show() {
        stage.show();
    }
    /*private LocalDate[] getDateRange(String period) {
    LocalDate end = LocalDate.now();
    LocalDate start;

    switch (period) {
        case "Weekly":
            start = end.minusDays(7);
            break;
        case "Monthly":
            start = end.minusMonths(1);
            break;
        default:
            start = end.minusYears(1);
    }
    return new LocalDate[]{start, end};
}*/private LocalDate[] getDateRange(String period) {
    return new LocalDate[]{
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2026, 12, 31)
    };
}

private int getSum(Connection con, String sql, String category, LocalDate[] range) throws Exception {
    PreparedStatement ps = con.prepareStatement(sql);
    ps.setString(1, category);
    ps.setDate(2, Date.valueOf(range[0]));
    ps.setDate(3, Date.valueOf(range[1]));
    ResultSet rs = ps.executeQuery();
    return rs.next() ? rs.getInt(1) : 0;
}
private Map<String, model.ReportData> loadReport(String period) {
    Map<String, model.ReportData> map = new HashMap<>();
    LocalDate[] range = getDateRange(period);

    try (Connection con = db.DBConnection.getConnection()) {

        for (String cat : categories) {
            model.ReportData rd = new model.ReportData();

         rd.purchase = getSum(con,
    """
    SELECT COALESCE(SUM(b.qty_ordered),0)
    FROM bill_invoice b
    JOIN product p ON b.pid = p.pid
    JOIN product_type pt ON p.ptype_id = pt.ptype_id
    WHERE pt.ptype_name = ?
    AND b.date BETWEEN ? AND ?
    """,
    cat, range
);

rd.issued = getSum(con,
    """
    SELECT COALESCE(SUM(i.qty_issued),0)
    FROM issue i
    JOIN product p ON i.pid = p.pid
    JOIN product_type pt ON p.ptype_id = pt.ptype_id
    WHERE pt.ptype_name = ?
    AND i.date BETWEEN ? AND ?
    """,
    cat, range
);

rd.returned = getSum(con,
    """
    SELECT COALESCE(SUM(r.quantity),0)
    FROM return_table r
    WHERE r.ptype_id = (
        SELECT ptype_id FROM product_type WHERE ptype_name = ?
    )
    AND r.date BETWEEN ? AND ?
    """,
    cat, range
);


            map.put(cat, rd);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    return map;
}
private VBox createCard(String title, Label valueLabel, String color) {

    Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

    valueLabel.setStyle("-fx-font-size: 26px; -fx-text-fill: white;");

    VBox box = new VBox(8, titleLabel, valueLabel);
    box.setPadding(new Insets(15));
    box.setAlignment(Pos.CENTER);
    box.setMinWidth(180);
  box.setStyle(
    "-fx-background-color: " + color + ";" +
    "-fx-background-radius: 18;" +
    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 15, 0.3, 0, 6);"
);
box.setOnMouseEntered(e -> {
    box.setScaleX(1.08);
    box.setScaleY(1.08);
    box.setStyle(
        "-fx-background-color: " + color + ";" +
        "-fx-background-radius: 18;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 25, 0.4, 0, 10);"
    );
});

box.setOnMouseExited(e -> {
    box.setScaleX(1);
    box.setScaleY(1);
    box.setStyle(
        "-fx-background-color: " + color + ";" +
        "-fx-background-radius: 18;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 15, 0.3, 0, 6);"
    );
});


    return box;
}
private BarChart<String, Number> createBarChart(Map<String, model.ReportData> data) {

    CategoryAxis xAxis = new CategoryAxis();
    xAxis.setLabel("Type");

    NumberAxis yAxis = new NumberAxis();
    yAxis.setLabel("Quantity");
    yAxis.setForceZeroInRange(true);

    BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
    chart.setTitle("Inventory Movement");
    chart.setPrefHeight(320);

    chart.setHorizontalGridLinesVisible(true);
    chart.setVerticalGridLinesVisible(false);
    chart.setStyle(
    "-fx-background-color: white;" +
    "-fx-background-radius: 18;" +
    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 18, 0.3, 0, 6);"
);


    int totalPurchase = 0;
    int totalIssued = 0;
    int totalReturned = 0;

    for (model.ReportData d : data.values()) {
        totalPurchase += d.purchase;
        totalIssued += d.issued;
        totalReturned += d.returned;
    }

    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.getData().add(new XYChart.Data<>("Purchase", totalPurchase));
    series.getData().add(new XYChart.Data<>("Issued", totalIssued));
    series.getData().add(new XYChart.Data<>("Returned", totalReturned));

   chart.getData().add(series);

String[] barColors = {
    "#1abc9c", // Purchase
    "#3498db", // Issued
    "#9b59b6"  // Returned
};

for (int idx = 0; idx < series.getData().size(); idx++) {
    int i = idx;
    XYChart.Data<String, Number> d = series.getData().get(idx);

    d.nodeProperty().addListener((obs, oldNode, newNode) -> {
        if (newNode != null) {
            newNode.setStyle(
                "-fx-background-radius: 14 14 0 0;" +
                "-fx-background-color: " + barColors[i] + ";"
            );
        }
    });
}

    displayBarValues(series);
    chart.setAnimated(true);
chart.setCategoryGap(40);
chart.setBarGap(10);


    return chart;
}

private VBox createCategoryPie() {

    PieChart pie = new PieChart();
    pie.setTitle("Stock by Category");
    pie.setLegendVisible(false);
    pie.setLabelsVisible(false);
    pie.setPrefSize(320, 320);



    Map<String, String> colorMap = Map.of(
        "Stationery", "#3498db",
        "Furniture", "#f39c12",
        "Computer", "#e74c3c",
        "Miscellaneous", "#2ecc71"
    );

    int totalStock = 0;

    try (Connection con = db.DBConnection.getConnection()) {

        String sql = """
            SELECT pt.ptype_name, COALESCE(SUM(p.qty_in_stock), 0)
            FROM product_type pt
            LEFT JOIN product p ON pt.ptype_id = p.ptype_id
            GROUP BY pt.ptype_name
        """;

        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String name = rs.getString(1);
            int value = rs.getInt(2);

            if (name.equalsIgnoreCase("Misc") || name.equalsIgnoreCase("Others")) {
                name = "Miscellaneous";
            }

            totalStock += value;

            PieChart.Data data = new PieChart.Data(name, value);
            pie.getData().add(data);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    // Apply fixed colors AFTER nodes are created
 pie.getData().forEach(d -> {
    d.nodeProperty().addListener((obs, oldNode, newNode) -> {
        if (newNode != null) {
            newNode.setStyle(
                "-fx-pie-color: " +
                colorMap.getOrDefault(d.getName(), "#bdc3c7") + ";"
            );
        }
    });
});

// -------- DONUT EFFECT --------
Label centerLabel = new Label(
    "TOTAL STOCK\n" + totalStock
);

centerLabel.setAlignment(Pos.CENTER);
centerLabel.setStyle(
    "-fx-font-size: 16px;" +
    "-fx-font-weight: bold;" +
    "-fx-text-fill: #34495e;" +
    "-fx-background-color: white;" +
    "-fx-background-radius: 50;" +
    "-fx-padding: 12 18 12 18;" +
    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0.3, 0, 2);"
);


// Custom legend
VBox legend = new VBox(10,
    createLegendItem("#3498db", "Stationery"),
    createLegendItem("#f39c12", "Furniture"),
    createLegendItem("#e74c3c", "Computer"),
    createLegendItem("#2ecc71", "Miscellaneous")
);
legend.setAlignment(Pos.CENTER_LEFT);

// Donut
StackPane donut = new StackPane();
javafx.scene.shape.Circle hole = new javafx.scene.shape.Circle(85);
hole.setStyle("-fx-fill: #ffffff;");
hole.setStyle("-fx-fill: #f4f6f9;");

donut.getChildren().addAll(pie, hole, centerLabel);
donut.setPadding(new Insets(10));

// Container
VBox container = new VBox(15, donut, legend);
container.setAlignment(Pos.CENTER);

return container;
}
private HBox createLegendItem(String color, String text) {
   Label dot = new Label();
dot.setStyle(
    "-fx-background-color: " + color + ";" +
    "-fx-background-radius: 50%;" +
    "-fx-min-width: 14px;" +
    "-fx-min-height: 14px;"
);

Label label = new Label(text);
label.setStyle(
    "-fx-font-size: 14px;" +
    "-fx-text-fill: #2c3e50;" +
    "-fx-font-weight: 600;"
);


    return new HBox(8, dot, label);
}
private void displayBarValues(XYChart.Series<String, Number> series) {
    for (XYChart.Data<String, Number> data : series.getData()) {
        Label label = new Label(data.getYValue().toString());
    label.setStyle(
    "-fx-font-size: 14px;" +
    "-fx-font-weight: bold;" +
    "-fx-text-fill: #2c3e50;"
);


        label.setTranslateY(-10);

        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                ((javafx.scene.layout.StackPane) newNode)
                        .getChildren().add(label);
            }
        });
    }
}
}