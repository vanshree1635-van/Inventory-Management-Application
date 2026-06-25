
package ui;

import com.mycompany.inventory.Inventory;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.*;
import javafx.geometry.Pos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AddInventoryUI {

    private final Scene scene;

    // Field-level reference so reloadOrderDropdown() can be called publicly
    private final ComboBox<String> orderNo = new ComboBox<>();

    public AddInventoryUI(Inventory app) {

        GridPane grid = new GridPane();
        Label formTitle = new Label("BILL RECEIVED DETAILS");
        formTitle.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 26));
        formTitle.setStyle("-fx-text-fill: #3E2723;");

        grid.setPadding(new Insets(30));
        grid.setHgap(40);
        grid.setVgap(18);
        grid.setStyle("-fx-background-color: #E2C49F;");

        ColumnConstraints col1 = new ColumnConstraints(); col1.setPrefWidth(220);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPrefWidth(300);
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPrefWidth(220);
        ColumnConstraints col4 = new ColumnConstraints(); col4.setPrefWidth(300);
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

        orderNo.setEditable(false);
        orderNo.setPrefSize(320, 42);
        orderNo.setStyle(fieldStyle);
        orderNo.setOnMouseEntered(e -> orderNo.setStyle(fieldStyle + "-fx-border-color: #8B5E34;"));
        orderNo.setOnMouseExited(e  -> orderNo.setStyle(fieldStyle));

        Label billLbl = new Label("Bill No");
        billLbl.setFont(labelFont);

        Label amountLbl = new Label("Bill Amount");
        amountLbl.setFont(labelFont);

        TextField billAmount = new TextField();
        billAmount.setPrefSize(320, 42);
        billAmount.setStyle(fieldStyle);

        TextField billNo = new TextField();
        billNo.setPrefSize(320, 42);
        billNo.setStyle(fieldStyle);

        grid.add(orderNoLbl, 0, 0); grid.add(orderNo,    1, 0);
        grid.add(billLbl,    0, 1); grid.add(billNo,     1, 1);

        // ---------------- RIGHT SIDE ----------------
        Label dateLbl = new Label("Received Date");
        dateLbl.setFont(labelFont);

        DatePicker orderDate = new DatePicker();
        orderDate.setPrefSize(320, 42);
        orderDate.setStyle(fieldStyle);

        Label qtyLbl = new Label("Quantity Received");
        qtyLbl.setFont(labelFont);

        TextField qtyReceived = new TextField();
        qtyReceived.setPrefSize(320, 42);
        qtyReceived.setStyle(fieldStyle);

        grid.add(amountLbl,  2, 0); grid.add(billAmount, 3, 0);
        grid.add(dateLbl,    2, 1); grid.add(orderDate,  3, 1);
        grid.add(qtyLbl,     2, 2); grid.add(qtyReceived,3, 2);

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

        save.setStyle(buttonStyle); save.setPrefSize(180, 50);
        back.setStyle(buttonStyle); back.setPrefSize(180, 50);

        back.setOnAction(e -> app.showDashboard());

        HBox buttonBox = new HBox(40);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(save, back);
        grid.add(buttonBox, 2, 6, 2, 1);

        // ---------------- DATABASE LOGIC ----------------
        save.setOnAction(e -> {
            try (Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/inventory_system",
                    "root",
                    "van#123"))  {

                // --- Validation ---
                if (orderNo.getValue() == null || orderNo.getValue().trim().isEmpty())
                    throw new Exception("Select Order No");
                if (billNo.getText().trim().isEmpty())
                    throw new Exception("Enter Bill No");
                if (billAmount.getText().trim().isEmpty())
                    throw new Exception("Enter Bill Amount");
                if (qtyReceived.getText().trim().isEmpty())
                    throw new Exception("Enter Quantity Received");

                int qty = Integer.parseInt(qtyReceived.getText().trim());
                if (qty <= 0)
                    throw new Exception("Quantity must be greater than 0");
                if (orderDate.getValue() == null)
                    throw new Exception("Select Received Date");

                // --- Get order details ---
                PreparedStatement getOrder = con.prepareStatement(
                    "SELECT entry_id, pid, sid, qty_ordered, order_status " +
                    "FROM order_table " +
                    "WHERE order_no = ? AND record_status = 'ACTIVE' LIMIT 1");
                getOrder.setString(1, orderNo.getValue().trim());
                ResultSet rs = getOrder.executeQuery();
                if (!rs.next())
                    throw new Exception("Order not found!");

                String orderStatus = rs.getString("order_status");
                if ("PAID".equals(orderStatus))
                    throw new Exception("Order '" + orderNo.getValue() +
                            "' is already PAID. Cannot add more bills.");

                int    entryId    = rs.getInt("entry_id");
                String pid        = rs.getString("pid");
                int    sid        = rs.getInt("sid");
                int    qtyOrdered = rs.getInt("qty_ordered");

                // ✅ UPPER LIMIT CHECK
                // How much has already been received for this order?
                PreparedStatement calcAlready = con.prepareStatement(
                    "SELECT IFNULL(SUM(qty_received), 0) AS already_received " +
                    "FROM bill_invoice " +
                    "WHERE entry_id = ? AND record_status = 'ACTIVE'");
                calcAlready.setInt(1, entryId);
                ResultSet rsAlready = calcAlready.executeQuery();
                rsAlready.next();
                int alreadyReceived = rsAlready.getInt("already_received");
                int qtyRemaining    = qtyOrdered - alreadyReceived;

                if (qty > qtyRemaining) {
                    throw new Exception(
                        "Quantity exceeds remaining order limit!\n" +
                        "  Order Qty    : " + qtyOrdered + "\n" +
                        "  Already Rcvd : " + alreadyReceived + "\n" +
                        "  Remaining    : " + qtyRemaining + "\n" +
                        "  You Entered  : " + qty
                    );
                }

                // --- Free up bill_no if a soft-deleted record holds it ---
                PreparedStatement freeBillNo = con.prepareStatement(
                    "UPDATE bill_invoice " +
                    "SET bill_no = CONCAT('DELETED_', bill_id, '_', bill_no) " +
                    "WHERE bill_no = ? AND record_status = 'INACTIVE'");
                freeBillNo.setString(1, billNo.getText().trim());
                freeBillNo.executeUpdate();

                // --- Insert bill ---
                PreparedStatement insertBill = con.prepareStatement(
                    "INSERT INTO bill_invoice " +
                    "(bill_no, bill_received_by, bill_amount, " +
                    " pid, sid, entry_id, qty_received, received_date, " +
                    " bill_status, record_status) " +
                    "VALUES (?, 'Inventory Manager', ?, ?, ?, ?, ?, ?, 'INCOMPLETE', 'ACTIVE')");
                insertBill.setString(1, billNo.getText().trim());
                insertBill.setDouble(2, Double.parseDouble(billAmount.getText().trim()));
                insertBill.setString(3, pid);
                insertBill.setInt(4, sid);
                insertBill.setInt(5, entryId);
                insertBill.setInt(6, qty);
                insertBill.setDate(7, java.sql.Date.valueOf(orderDate.getValue()));
                insertBill.executeUpdate();

                // --- Update product stock ---
                PreparedStatement updateStock = con.prepareStatement(
                    "UPDATE product SET qty_in_stock = qty_in_stock + ? WHERE pid = ?");
                updateStock.setInt(1, qty);
                updateStock.setString(2, pid);
                updateStock.executeUpdate();

                // --- Calculate total received now (Java-side, no subquery conflict) ---
                int totalReceived   = alreadyReceived + qty;
                String newBillStatus  = (totalReceived >= qtyOrdered) ? "COMPLETE"   : "INCOMPLETE";
                String newOrderStatus = (totalReceived >= qtyOrdered) ? "PAID"       : "IN_PROCESS";

                // --- Plain UPDATE bill_status ---
                PreparedStatement updateBillStatus = con.prepareStatement(
                    "UPDATE bill_invoice SET bill_status = ? " +
                    "WHERE entry_id = ? AND record_status = 'ACTIVE'");
                updateBillStatus.setString(1, newBillStatus);
                updateBillStatus.setInt(2, entryId);
                updateBillStatus.executeUpdate();

                // --- Plain UPDATE order_status ---
                PreparedStatement updateOrderStatus = con.prepareStatement(
                    "UPDATE order_table SET order_status = ? WHERE entry_id = ?");
                updateOrderStatus.setString(1, newOrderStatus);
                updateOrderStatus.setInt(2, entryId);
                updateOrderStatus.executeUpdate();

                new Alert(Alert.AlertType.INFORMATION, "Bill added successfully").show();

                // Reset form; reload dropdown (PAID orders disappear automatically)
                reloadOrderDropdown();
                orderNo.setValue(null);
                billNo.clear();
                billAmount.clear();
                qtyReceived.clear();
                orderDate.setValue(null);

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
            }
        });

        // ---------------- LAYOUT ----------------
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

        // Load dropdown immediately on first construction
        reloadOrderDropdown();
    }

    // =====================================================================
    //  PUBLIC — call this in Inventory.java before setScene() so the
    //  dropdown is always fresh when the user navigates here.
    //  e.g.:
    //    addInventoryUI.reloadOrderDropdown();
    //    primaryStage.setScene(addInventoryUI.getScene());
    // =====================================================================
    public void reloadOrderDropdown() {
        orderNo.getItems().clear();
        orderNo.setValue(null);
        String query =
            "SELECT order_no FROM order_table " +
            "WHERE order_status = 'IN_PROCESS' AND record_status = 'ACTIVE' " +
            "GROUP BY order_no " +
            "ORDER BY order_no";
        try (Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/inventory_system",
                "root",
                "van#123");  //password
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                orderNo.getItems().add(rs.getString(1));
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load orders:\n" + e.getMessage()).show();
        }
    }

    public Scene getScene() {
        return scene;
    }
}
