package ui;

import com.mycompany.inventory.Inventory;
import db.DBConnection;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import model.Product;
import model.IssueRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReturnUI {

    private Scene scene;
    private Inventory app;
    private String returnType;

    private TableView<IssueRow> table = new TableView<>();
    private VBox issueRoot;
    private DatePicker sharedReturnDate;

    public static final String ISSUE_TO_INVENTORY    = "ISSUE_TO_INVENTORY";
    public static final String INVENTORY_TO_SUPPLIER = "INVENTORY_TO_SUPPLIER";

    public ReturnUI(Inventory app, String type) {

        this.app        = app;
        this.returnType = type;

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));

        Label title = new Label("RETURN MANAGEMENT");
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold;");

        root.getChildren().add(title);

        if (returnType.equals(ISSUE_TO_INVENTORY)) {

            sharedReturnDate = new DatePicker(LocalDate.now());
            issueRoot = root;

            // Only title(0) + date-label(1) + DatePicker(2) kept at top
            // Back button lives inside buildIssueReturnTable next to Save
            root.getChildren().addAll(new Label("Return Date"), sharedReturnDate);

            buildIssueReturnTable(root);

        } else if (returnType.equals(INVENTORY_TO_SUPPLIER)) {
            root.getChildren().add(buildRTSForm());
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;");

        scene = new Scene(scroll, 1000, 700);
    }

    public Scene getScene() {
        return scene;
    }

    // =====================================================================
    //  SECTION 1 : ISSUE → INVENTORY  (table: return_table)
    // =====================================================================

    private void buildIssueReturnTable(VBox parent) {

        table.getColumns().clear();
        table.getItems().clear();

        ObservableList<IssueRow> list = FXCollections.observableArrayList();

        // Map to store product_name keyed by issue_id since IssueRow may not have a productName field
        Map<Integer, String> productNameMap = new HashMap<>();

        String sql = "SELECT i.issue_id, i.pid, p.product_name, i.issue_to, i.issued_by, i.dept_name, " +
             "       i.qty_issued, i.qty_returned, i.date " +
             "FROM issue i " +
             "JOIN product p ON i.pid = p.pid " +
             "WHERE i.qty_issued > i.qty_returned";

        try (Connection con = DBConnection.getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            while (rs.next()) {
                IssueRow row = new IssueRow(
                        rs.getInt   ("issue_id"),
                        rs.getString("pid"),
                        rs.getString("issue_to"),
                        rs.getString("issued_by"),
                        rs.getString("dept_name"),
                        rs.getInt   ("qty_issued"),
                        rs.getDate  ("date").toString()
                );
                row.setAlreadyReturned(rs.getInt("qty_returned"));
                productNameMap.put(rs.getInt("issue_id"), rs.getString("product_name"));
                list.add(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ---- columns -------------------------------------------------------

        TableColumn<IssueRow, String> prodCol = new TableColumn<>("Product ID");
        prodCol.setCellValueFactory(c -> c.getValue().pidProperty());
        prodCol.setPrefWidth(100);

        // ---- NEW: Product Name column --------------------------------------
        TableColumn<IssueRow, String> prodNameCol = new TableColumn<>("Product Name");
        prodNameCol.setPrefWidth(160);
        prodNameCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        productNameMap.getOrDefault(c.getValue().getIssueId(), "—")));

        TableColumn<IssueRow, String> toCol = new TableColumn<>("Issued To");
        toCol.setCellValueFactory(c -> c.getValue().issueToProperty());
        toCol.setPrefWidth(120);

        TableColumn<IssueRow, String> deptCol = new TableColumn<>("Dept");
        deptCol.setCellValueFactory(c -> c.getValue().deptProperty());
        deptCol.setPrefWidth(120);

        TableColumn<IssueRow, Integer> qtyIssuedCol =
                new TableColumn<>("Qty Issued");
        qtyIssuedCol.setCellValueFactory(c ->
                c.getValue().qtyIssuedProperty().asObject());
        qtyIssuedCol.setPrefWidth(90);

        TableColumn<IssueRow, Integer> alreadyReturnedCol =
                new TableColumn<>("Already Returned");
        alreadyReturnedCol.setCellValueFactory(c ->
                c.getValue().alreadyReturnedProperty().asObject());
        alreadyReturnedCol.setPrefWidth(130);

        // Embedded TextField — writes to model on every keystroke
        TableColumn<IssueRow, Integer> returnQtyCol =
                new TableColumn<>("Qty to Return");
        returnQtyCol.setPrefWidth(110);
        returnQtyCol.setCellValueFactory(c ->
                c.getValue().qtyReturnProperty().asObject());
        returnQtyCol.setCellFactory(col ->
                new TableCell<IssueRow, Integer>() {
            private final TextField tf = new TextField();

            {
                tf.setPrefWidth(80);
                tf.textProperty().addListener((obs, oldVal, newVal) -> {
                    IssueRow row = getTableRow() != null
                            ? (IssueRow) getTableRow().getItem() : null;
                    if (row == null) return;
                    try {
                        row.setQtyReturn(Integer.parseInt(newVal.trim()));
                    } catch (NumberFormatException ignored) { }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null ||
                        getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    IssueRow row = (IssueRow) getTableRow().getItem();
                    int modelVal = row.getQtyReturn();
                    String current = tf.getText().trim();
                    if (!current.equals(String.valueOf(modelVal))) {
                        tf.setText(modelVal > 0
                                ? String.valueOf(modelVal) : "");
                    }
                    setGraphic(tf);
                }
            }
        });

        // Custom CheckBox cell — works without setEditable(true)
        TableColumn<IssueRow, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setPrefWidth(70);
        selectCol.setCellValueFactory(c -> c.getValue().selectedProperty());
        selectCol.setCellFactory(col ->
        new TableCell<IssueRow, Boolean>() {
    private final CheckBox cb = new CheckBox();

    {
        cb.setOnAction(e -> {
            IssueRow row = getTableRow() != null
                    ? (IssueRow) getTableRow().getItem() : null;
            if (row != null) {
                // Write directly into the property instead of a setter
                row.selectedProperty().set(cb.isSelected());
            }
        });
    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getTableRow() == null ||
                getTableRow().getItem() == null) {
            setGraphic(null);
        } else {
            IssueRow row = (IssueRow) getTableRow().getItem();
            cb.setSelected(row.selectedProperty().get());
            row.selectedProperty().addListener(
                    (obs, oldVal, newVal) -> cb.setSelected(newVal));
            setGraphic(cb);
        }
    }
});

        table.getColumns().addAll(
                prodCol, prodNameCol, toCol, deptCol,
                qtyIssuedCol, alreadyReturnedCol,
                returnQtyCol, selectCol);
        table.setItems(list);
        table.setPrefHeight(400);
        table.setPlaceholder(new Label("No pending returns found."));

        // ---- Buttons: Save Return + Back side by side ----------------------

        Button save = new Button("SAVE RETURN");
        save.setStyle("""
            -fx-background-color: #4CAF50;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-padding: 10 30 10 30;
        """);

        Button back = new Button("BACK");
        back.setStyle("""
            -fx-background-color: #757575;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-padding: 10 30 10 30;
        """);
        back.setOnAction(e -> app.showDashboard());

        HBox btnRow = new HBox(20, save, back);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        // ---- Save logic ----------------------------------------------------

        save.setOnAction(e -> {

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                boolean anySelected = false;

                for (IssueRow row : list) {

                    if (!row.isSelected()) continue;
                    anySelected = true;

                    int qtyToReturn = row.getQtyReturn();

                    if (qtyToReturn <= 0) {
                        con.rollback();
                        new Alert(Alert.AlertType.ERROR,
                                "Please enter a quantity greater than 0 for: "
                                + row.getPid()).show();
                        return;
                    }

                    int maxReturnable =
                            row.getQtyIssued() - row.getAlreadyReturned();
                    if (qtyToReturn > maxReturnable) {
                        con.rollback();
                        new Alert(Alert.AlertType.ERROR,
                                "Return qty (" + qtyToReturn +
                                ") exceeds remaining returnable qty (" +
                                maxReturnable + ") for: "
                                + row.getPid()).show();
                        return;
                    }

                    // 1. Insert into return_table
                    PreparedStatement psInsert = con.prepareStatement(
                            "INSERT INTO return_table " +
                            "  (ptype_id, issue_id, pid, quantity, return_to, date) " +
                            "VALUES " +
                            "  ((SELECT ptype_id FROM product WHERE pid = ?), " +
                            "   ?, ?, ?, 'Inventory', ?)");
                    psInsert.setString(1, row.getPid());
                    psInsert.setInt   (2, row.getIssueId());
                    psInsert.setString(3, row.getPid());
                    psInsert.setInt   (4, qtyToReturn);
                    psInsert.setDate  (5,
                            Date.valueOf(sharedReturnDate.getValue()));
                    psInsert.executeUpdate();

                    // 2. Increase product stock
                    PreparedStatement psStock = con.prepareStatement(
                            "UPDATE product " +
                            "SET    qty_in_stock = qty_in_stock + ? " +
                            "WHERE  pid = ?");
                    psStock.setInt   (1, qtyToReturn);
                    psStock.setString(2, row.getPid());
                    psStock.executeUpdate();

                    // 3. Update qty_returned on the issue row
                    PreparedStatement psIssue = con.prepareStatement(
                            "UPDATE issue " +
                            "SET    qty_returned = qty_returned + ? " +
                            "WHERE  issue_id = ?");
                    psIssue.setInt(1, qtyToReturn);
                    psIssue.setInt(2, row.getIssueId());
                    psIssue.executeUpdate();
                }

                if (!anySelected) {
                    new Alert(Alert.AlertType.WARNING,
                            "Please select at least one row to return.").show();
                    return;
                }

                con.commit();
                new Alert(Alert.AlertType.INFORMATION,
                        "Return saved successfully!").show();

                // Keep title(0) + date-label(1) + DatePicker(2), remove rest
                while (parent.getChildren().size() > 3) {
                    parent.getChildren().remove(
                            parent.getChildren().size() - 1);
                }
                buildIssueReturnTable(parent);

            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR,
                        "Error saving return:\n" + ex.getMessage()).show();
            }
        });

        parent.getChildren().addAll(table, btnRow);
    }

    // =====================================================================
    //  SECTION 2 : INVENTORY → SUPPLIER  (table: rts_table)
    // =====================================================================

    private VBox buildRTSForm() {

        DatePicker rtsDate = new DatePicker(LocalDate.now());

        GridPane grid = new GridPane();
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

        // ---- LEFT column ---------------------------------------------------

        Label productLbl = new Label("Product Name");
        productLbl.setFont(labelFont);
        ComboBox<Product> productBox = new ComboBox<>();
        productBox.setPrefSize(320, 42);
        productBox.setStyle(fieldStyle);
        loadProducts(productBox);

        Label qtyLbl = new Label("Quantity");
        qtyLbl.setFont(labelFont);
        TextField qtyField = new TextField();
        qtyField.setPrefSize(320, 42);
        qtyField.setStyle(fieldStyle);

        Label billLbl = new Label("Bill (Invoice No.)");
        billLbl.setFont(labelFont);
        ComboBox<BillItem> billBox = new ComboBox<>();
        billBox.setPrefSize(320, 42);
        billBox.setStyle(fieldStyle);

        grid.add(productLbl, 0, 0); grid.add(productBox, 1, 0);
        grid.add(qtyLbl,     0, 1); grid.add(qtyField,   1, 1);
        grid.add(billLbl,    0, 2); grid.add(billBox,     1, 2);

        // ---- RIGHT column --------------------------------------------------

        Label statusLbl = new Label("Status");
        statusLbl.setFont(labelFont);
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("SENT", "RECEIVED");
        statusBox.setValue("SENT");
        statusBox.setPrefSize(320, 42);
        statusBox.setStyle(fieldStyle);

        Label stockHintLbl = new Label("Current stock: —");
        stockHintLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        stockHintLbl.setStyle("-fx-text-fill: #6B4226;");

        Label dateLbl = new Label("RTS Date");
        dateLbl.setFont(labelFont);
        rtsDate.setPrefSize(320, 42);
        rtsDate.setStyle(fieldStyle);

        grid.add(statusLbl,    2, 0); grid.add(statusBox, 3, 0);
        grid.add(dateLbl,      2, 1); grid.add(rtsDate,   3, 1);
        grid.add(stockHintLbl, 2, 2, 2, 1);

        // ---- Refresh stock hint (reused after save too) --------------------

        Runnable refreshStockHint = () -> {
            Product sel = productBox.getValue();
            if (sel == null) {
                stockHintLbl.setText("Current stock: —");
                return;
            }
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT qty_in_stock FROM product WHERE pid = ?")) {
                ps.setString(1, sel.getPid());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stockHintLbl.setText(
                            "Current stock: " + rs.getInt("qty_in_stock"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        // ---- Product selection → refresh bills + stock hint ----------------

        productBox.setOnAction(e -> {
            billBox.getItems().clear();
            Product sel = productBox.getValue();
            if (sel == null) {
                stockHintLbl.setText("Current stock: —");
                return;
            }
            loadBillsForProduct(sel.getPid(), billBox);
            refreshStockHint.run();
        });

        // ---- Buttons -------------------------------------------------------

        Button saveBtn = new Button("SEND TO SUPPLIER");
        Button backBtn = new Button("BACK");

        String buttonStyle = """
            -fx-background-color: #8B5E34;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-padding: 12 35 12 35;
        """;
        saveBtn.setStyle(buttonStyle);
        backBtn.setStyle(buttonStyle);
        saveBtn.setPrefSize(180, 50);
        backBtn.setPrefSize(180, 50);

        HBox buttonBox = new HBox(40, saveBtn, backBtn);
        buttonBox.setAlignment(Pos.CENTER);
        grid.add(buttonBox, 2, 3, 2, 1);

        backBtn.setOnAction(e -> app.showDashboard());

        // ---- Save / DB logic -----------------------------------------------

        saveBtn.setOnAction(e -> {

            Product  selectedProduct = productBox.getValue();
            BillItem selectedBill    = billBox.getValue();
            String   statusVal       = statusBox.getValue();

            if (selectedProduct == null || selectedBill == null ||
                    qtyField.getText().isEmpty() || statusVal == null ||
                    rtsDate.getValue() == null) {
                new Alert(Alert.AlertType.ERROR,
                        "All fields are mandatory.").show();
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(qtyField.getText().trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Enter a valid positive quantity.").show();
                return;
            }

            // Fetch live stock
            int currentStock;
            try (Connection conCheck = DBConnection.getConnection();
                 PreparedStatement psCheck = conCheck.prepareStatement(
                         "SELECT qty_in_stock FROM product WHERE pid = ?")) {
                psCheck.setString(1, selectedProduct.getPid());
                ResultSet rsCheck = psCheck.executeQuery();
                if (rsCheck.next()) {
                    currentStock = rsCheck.getInt("qty_in_stock");
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "Product not found in database.").show();
                    return;
                }
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Could not verify stock: " + ex.getMessage()).show();
                return;
            }

            // Only block if sending out more than available
            if (statusVal.equals("SENT") && quantity > currentStock) {
                new Alert(Alert.AlertType.ERROR,
                        "Quantity (" + quantity +
                        ") exceeds current stock (" + currentStock +
                        ").\nCannot send more than what is in stock.").show();
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                PreparedStatement psRts = con.prepareStatement(
                        "INSERT INTO rts_table " +
                        "  (bill_id, pid, sid, quantity, rts_date, record_status) " +
                        "VALUES (?, ?, ?, ?, ?, ?)");
                psRts.setInt   (1, selectedBill.getBillId());
                psRts.setString(2, selectedProduct.getPid());
                psRts.setInt   (3, selectedBill.getSid());
                psRts.setInt   (4, quantity);
                psRts.setDate  (5, Date.valueOf(rtsDate.getValue()));
                psRts.setString(6, statusVal);
                psRts.executeUpdate();

                // SENT → subtract, RECEIVED → add
                String stockSql = statusVal.equals("SENT")
                        ? "UPDATE product SET qty_in_stock = qty_in_stock - ? WHERE pid = ?"
                        : "UPDATE product SET qty_in_stock = qty_in_stock + ? WHERE pid = ?";

                PreparedStatement psStock = con.prepareStatement(stockSql);
                psStock.setInt   (1, quantity);
                psStock.setString(2, selectedProduct.getPid());
                psStock.executeUpdate();

                con.commit();

                new Alert(Alert.AlertType.INFORMATION,
                        statusVal.equals("SENT")
                            ? "RTS saved. Stock decreased by " + quantity + "."
                            : "RTS saved. Stock increased by " + quantity +
                              " (goods received back from supplier)."
                ).show();

                // Reset form
                productBox.setValue(null);
                billBox.getItems().clear();
                qtyField.clear();
                statusBox.setValue("SENT");
                rtsDate.setValue(LocalDate.now());

                // Instant refresh: reload product list + hint label
                loadProducts(productBox);
                refreshStockHint.run();

            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR,
                        "Error saving RTS:\n" + ex.getMessage()).show();
            }
        });

        // ---- Card layout ---------------------------------------------------

        VBox cardContent = new VBox(30);
        cardContent.setAlignment(Pos.TOP_CENTER);
        Label formTitle = new Label("RETURN TO SUPPLIER (RTS)");
        formTitle.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        cardContent.getChildren().addAll(formTitle, grid);

        StackPane card = new StackPane(cardContent);
        card.setPadding(new Insets(40));
        card.setStyle("""
            -fx-background-color: rgba(255,255,255,0.92);
            -fx-background-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0.4, 0, 10);
        """);

        VBox root = new VBox();
        root.setPadding(new Insets(50));
        root.setSpacing(35);
        root.setStyle("-fx-background-color: #E2C49F;");
        root.getChildren().add(card);
        return root;
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    private void loadProducts(ComboBox<Product> box) {
        Product previously = box.getValue();
        ObservableList<Product> list = FXCollections.observableArrayList();
        try (Connection con = DBConnection.getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(
                     "SELECT pid, product_name, qty_in_stock FROM product")) {
            while (rs.next()) {
                list.add(new Product(
                        rs.getString("pid"),
                        rs.getString("product_name"),
                        rs.getInt   ("qty_in_stock")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        box.setItems(list);
        if (previously != null) {
            list.stream()
                .filter(p -> p.getPid().equals(previously.getPid()))
                .findFirst()
                .ifPresent(box::setValue);
        }
    }

    private void loadBillsForProduct(String pid, ComboBox<BillItem> box) {
        ObservableList<BillItem> list = FXCollections.observableArrayList();
        String sql = "SELECT bill_id, bill_no, sid " +
                     "FROM   bill_invoice " +
                     "WHERE  pid = ? AND record_status = 'ACTIVE'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new BillItem(
                        rs.getInt   ("bill_id"),
                        rs.getString("bill_no"),
                        rs.getInt   ("sid")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        box.setItems(list);
    }

    // =====================================================================
    //  Inner helper class
    // =====================================================================

    public static class BillItem {
        private final int    billId;
        private final String billNo;
        private final int    sid;

        public BillItem(int billId, String billNo, int sid) {
            this.billId = billId;
            this.billNo = billNo;
            this.sid    = sid;
        }

        public int    getBillId() { return billId; }
        public String getBillNo() { return billNo; }
        public int    getSid()    { return sid;    }

        @Override
        public String toString() { return billNo; }
    }
}
