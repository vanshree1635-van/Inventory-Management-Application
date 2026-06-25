
package ui;

import com.mycompany.inventory.Inventory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.*;

public class AddOrderUI {

    private final Scene scene;

    public AddOrderUI(Inventory app) {

        GridPane grid = new GridPane();
        Label formTitle = new Label("ADD ORDER FORM");
        formTitle.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 26));
        formTitle.setStyle("-fx-text-fill: #3E2723;");

        grid.setPadding(new Insets(30));
        grid.setHgap(40);
        grid.setVgap(18);
        grid.setStyle("-fx-background-color: #E2C49F;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(220);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(300);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPrefWidth(220);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPrefWidth(300);

        grid.getColumnConstraints().addAll(col1, col2, col3, col4);

        Font labelFont = Font.font("Arial", FontWeight.BOLD, 16);

        String fieldStyle = """
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: #C49A6C;
            -fx-border-width: 1.5;
            -fx-background-color: #FFF6E9;
            -fx-font-size: 15px;
        """;

        // ---------------- LEFT SIDE ----------------

        Label orderNoLbl = new Label("Order No");
        orderNoLbl.setFont(labelFont);

        Label productLbl = new Label("Product Name");
        productLbl.setFont(labelFont);

        Label supplierLbl = new Label("Supplier Name");
        supplierLbl.setFont(labelFont);

        TextField orderNo = new TextField();
        orderNo.setPrefSize(320, 42);
        orderNo.setStyle(fieldStyle);

        ComboBox<String> productName = new ComboBox<>();
        ComboBox<String> supplierName = new ComboBox<>();

        setupComboBox(productName,
                "SELECT DISTINCT product_name FROM product",
                fieldStyle);

        setupComboBox(supplierName,
                "SELECT DISTINCT name FROM supplier",
                fieldStyle);

        grid.add(orderNoLbl, 0, 0);
        grid.add(orderNo, 1, 0);

        grid.add(productLbl, 0, 1);
        grid.add(productName, 1, 1);

        grid.add(supplierLbl, 0, 2);
        grid.add(supplierName, 1, 2);

        // ---------------- RIGHT SIDE ----------------

        Label dateLbl = new Label("Order Date");
        dateLbl.setFont(labelFont);

        DatePicker orderDate = new DatePicker();
        orderDate.setPrefSize(320, 42);
        orderDate.setStyle(fieldStyle);

        Label qtyLbl = new Label("Quantity Ordered");
        qtyLbl.setFont(labelFont);

        TextField qtyOrdered = new TextField();
        qtyOrdered.setPrefSize(320, 42);
        qtyOrdered.setStyle(fieldStyle);

        Label statusLbl = new Label("Order Status");
        statusLbl.setFont(labelFont);

        Label orderStatusLabel = new Label("IN_PROCESS");
        orderStatusLabel.setPrefSize(320, 42);
        orderStatusLabel.setStyle(fieldStyle + "-fx-padding: 10 14 10 14;");
        orderStatusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));

        grid.add(dateLbl, 2, 0);
        grid.add(orderDate, 3, 0);

        grid.add(qtyLbl, 2, 1);
        grid.add(qtyOrdered, 3, 1);

        grid.add(statusLbl, 2, 2);
        grid.add(orderStatusLabel, 3, 2);

        // ---------------- BUTTONS ----------------

        Button save = new Button("SAVE");
        Button back = new Button("BACK");

        String buttonStyle = """
            -fx-background-color: #8B5E34;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-padding: 12 35 12 35;
        """;

        save.setStyle(buttonStyle);
        back.setStyle(buttonStyle);
        save.setPrefSize(180, 50);
        back.setPrefSize(180, 50);

        back.setOnAction(e -> app.showDashboard());

        HBox buttonBox = new HBox(40);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(save, back);
        grid.add(buttonBox, 2, 4, 2, 1);

        // ---------------- DATABASE LOGIC ----------------

        save.setOnAction(e -> {
            try (Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/inventory_system",
                    "root",
                    "van#123")) {

                if (orderNo.getText().trim().isEmpty())
                    throw new Exception("Enter Order No");

                if (productName.getValue() == null || productName.getValue().trim().isEmpty())
                    throw new Exception("Select a Product");

                if (supplierName.getValue() == null || supplierName.getValue().trim().isEmpty())
                    throw new Exception("Select a Supplier");

                if (qtyOrdered.getText().trim().isEmpty())
                    throw new Exception("Enter Quantity");

                if (orderDate.getValue() == null)
                    throw new Exception("Select Order Date");

                // Get PID
                PreparedStatement ps1 = con.prepareStatement(
                        "SELECT pid FROM product WHERE product_name=?");
                ps1.setString(1, productName.getValue());
                ResultSet rs1 = ps1.executeQuery();
                if (!rs1.next()) throw new Exception("Product not found in database");
                String pid = rs1.getString("pid");

                // Get SID
                PreparedStatement ps2 = con.prepareStatement(
                        "SELECT sid FROM supplier WHERE name=?");
                ps2.setString(1, supplierName.getValue());
                ResultSet rs2 = ps2.executeQuery();
                if (!rs2.next()) throw new Exception("Supplier not found in database");
                int sid = rs2.getInt("sid");

                // Check if this order_no already exists with PAID status
                PreparedStatement checkPaid = con.prepareStatement(
                    "SELECT order_status FROM order_table " +
                    "WHERE order_no=? AND record_status='ACTIVE' LIMIT 1");
                checkPaid.setString(1, orderNo.getText().trim());
                ResultSet rsPaid = checkPaid.executeQuery();
                if (rsPaid.next()) {
                    String existingStatus = rsPaid.getString("order_status");
                    if ("PAID".equals(existingStatus)) {
                        throw new Exception("Order No '" + orderNo.getText() +
                                "' is already PAID. Cannot reuse it.");
                    }
                }

                // INSERT INTO ORDER TABLE
                PreparedStatement insertOrder = con.prepareStatement(
                        "INSERT INTO order_table " +
                        "(order_no, pid, sid, order_date, qty_ordered, " +
                        " order_status, record_status) " +
                        "VALUES (?,?,?,?,?,'IN_PROCESS','ACTIVE')");

                insertOrder.setString(1, orderNo.getText().trim());
                insertOrder.setString(2, pid);
                insertOrder.setInt(3, sid);
                insertOrder.setDate(4, java.sql.Date.valueOf(orderDate.getValue()));
                insertOrder.setInt(5, Integer.parseInt(qtyOrdered.getText().trim()));
                insertOrder.executeUpdate();

                new Alert(Alert.AlertType.INFORMATION,
                        "Order added successfully").show();

                // Reset form
                orderNo.clear();
                productName.setValue(null);
                supplierName.setValue(null);
                qtyOrdered.clear();
                orderDate.setValue(null);

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
            }
        });

        VBox root = new VBox();
        root.setPadding(new Insets(50));
        root.setSpacing(35);
        root.setStyle("-fx-background-color: #E2C49F;");

        VBox cardContent = new VBox(30);
        cardContent.setAlignment(Pos.TOP_CENTER);
        cardContent.getChildren().addAll(formTitle, grid);

        StackPane card = new StackPane(cardContent);
        card.setPadding(new Insets(40));
        card.setStyle("""
            -fx-background-color: rgba(255,255,255,0.92);
            -fx-background-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0.4, 0, 10);
        """);

        root.getChildren().add(card);
        scene = new Scene(root, 1024, 768);
    }

    public Scene getScene() {
        return scene;
    }

    private void loadData(ComboBox<String> combo, String query) {
        try (Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/inventory_system",
                "root",
                "van#123");
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                combo.getItems().add(rs.getString(1));
            }

        } catch (Exception e) {
            // Surface the error so it's visible during development
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load dropdown data:\n" + e.getMessage()).show();
        }
    }

    private void setupComboBox(ComboBox<String> combo, String query, String fieldStyle) {

        combo.setEditable(false);   // ← changed from true: prevents the listener
        combo.setPrefSize(320, 42); //   from interfering with item selection
        combo.setStyle(fieldStyle);

        loadData(combo, query);

        combo.setOnMouseEntered(e ->
                combo.setStyle(fieldStyle + "-fx-border-color: #8B5E34;")
        );

        combo.setOnMouseExited(e ->
                combo.setStyle(fieldStyle)
        );
    }
}
