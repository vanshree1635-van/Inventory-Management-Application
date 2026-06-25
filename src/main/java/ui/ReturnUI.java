package ui;

import com.mycompany.inventory.Inventory;
import db.DBConnection;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;

import model.Product;
import model.IssueRow;

import java.sql.*;
import java.time.LocalDate;

public class ReturnUI{
    private Scene scene;
    private Inventory app;
    private String returnType;

    private TableView<IssueRow> table = new TableView<>();

    public static final String ISSUE_TO_INVENTORY = "ISSUE_TO_INVENTORY";
    public static final String INVENTORY_TO_SUPPLIER = "INVENTORY_TO_SUPPLIER";
    
    public ReturnUI(Inventory app,String type) {

        this.app = app;
        this.returnType = type;

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));

        Label title = new Label("RETURN MANAGEMENT");
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold;");


        DatePicker returnDate = new DatePicker(LocalDate.now());

        Button back = new Button("BACK");
        back.setOnAction(e -> app.showDashboard());
        
        root.getChildren().add(title);

        if (returnType.equals(ISSUE_TO_INVENTORY)) {
        if (returnType.equals(ISSUE_TO_INVENTORY)) {
        VBox issueBox = buildIssueReturnTable(returnDate);
        root.getChildren().addAll(new Label("Return Date"), returnDate, issueBox);
    }
        }
        else if (returnType.equals(INVENTORY_TO_SUPPLIER)) {
        root.getChildren().add(buildRTSForm(returnDate));
        }

        root.getChildren().add(back);
        scene = new Scene(root, 1000, 700);
        
    }

    public Scene getScene() {
        return scene;
    }

    // ================= RETURN FROM ISSUE =================

    private VBox buildIssueReturnTable(DatePicker returnDate) {

            table.getColumns().clear();
            ObservableList<IssueRow> list = FXCollections.observableArrayList();

            try (Connection con = DBConnection.getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT issue_id, pid, issue_to, issued_by, dept_name, qty_issued, date FROM issue")) {

                while (rs.next()) {
                    list.add(new IssueRow(
                            rs.getInt("issue_id"),
                            rs.getString("pid"),
                            rs.getString("issue_to"),
                            rs.getString("issued_by"),
                            rs.getString("dept_name"),
                            rs.getInt("qty_issued"),
                            rs.getDate("date").toString()
                    ));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            TableColumn<IssueRow,String> prodCol =
                    new TableColumn<>("Product ID");
            prodCol.setCellValueFactory(c -> c.getValue().pidProperty());

            TableColumn<IssueRow,String> toCol =
                    new TableColumn<>("Issued To");
            toCol.setCellValueFactory(c -> c.getValue().issueToProperty());

            TableColumn<IssueRow,String> deptCol =
                    new TableColumn<>("Dept");
            deptCol.setCellValueFactory(c -> c.getValue().deptProperty());

            TableColumn<IssueRow,Integer> qtyCol =
                    new TableColumn<>("Qty Issued");
            qtyCol.setCellValueFactory(c -> c.getValue().qtyIssuedProperty().asObject());

            TableColumn<IssueRow,Integer> returnQtyCol =
                    new TableColumn<>("Qty Returned");
            returnQtyCol.setCellValueFactory(c -> c.getValue().qtyReturnProperty().asObject());
            returnQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(
                    new javafx.util.converter.IntegerStringConverter()));
            returnQtyCol.setOnEditCommit(e ->
                    e.getRowValue().setQtyReturn(e.getNewValue()));

            TableColumn<IssueRow,Boolean> selectCol =
                    new TableColumn<>("Select");
            selectCol.setCellValueFactory(c -> c.getValue().selectedProperty());
            selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));

            table.getColumns().addAll(prodCol,toCol,deptCol,qtyCol,returnQtyCol,selectCol);
            table.setItems(list);
            table.setEditable(true);

            Button save = new Button("SAVE RETURN");

            save.setOnAction(e -> {

                try (Connection con = DBConnection.getConnection()) {

                    con.setAutoCommit(false);

                    for (IssueRow row : list) {

                        if (row.isSelected() && row.getQtyReturn() > 0) {

                            if (row.getQtyReturn() > row.getQtyIssued()) {
                                throw new Exception("Return qty > issued qty");
                            }

                            PreparedStatement ps =
                                    con.prepareStatement(
                                            "INSERT INTO return_table " +
                                            "(ptype_id, issue_id, pid, quantity, return_to, date) " +
                                            "VALUES ((SELECT ptype_id FROM product WHERE pid=?),?,?,?,?,?)");

                            ps.setString(1,row.getPid());
                            ps.setInt(2,row.getIssueId());
                            ps.setString(3,row.getPid());
                            ps.setInt(4,row.getQtyReturn());
                            ps.setString(5,"Inventory");
                            ps.setDate(6,Date.valueOf(returnDate.getValue()));
                            ps.executeUpdate();

                            PreparedStatement update =
                                    con.prepareStatement(
                                            "UPDATE product SET qty_in_stock = qty_in_stock + ? WHERE pid=?");
                            update.setInt(1,row.getQtyReturn());
                            update.setString(2,row.getPid());
                            update.executeUpdate();
                        }
                    }

                    con.commit();
                    new Alert(Alert.AlertType.INFORMATION,"Return Saved").show();

                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,"Error in return").show();
                }
            });

            table.setPrefHeight(400);
            table.setPlaceholder(new Label("No Issues Found"));

            VBox box = new VBox(10, table, save);
                box.setSpacing(10);
                table.setPrefHeight(400);
                return box;
            }

        // ================= RTS FORM =================

        private VBox buildRTSForm(DatePicker rtsDate) {

        GridPane grid = new GridPane();
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

        // ================= LEFT SIDE =================
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

        Label invoiceLbl = new Label("Invoice No.");
        invoiceLbl.setFont(labelFont);
        TextField invoiceField = new TextField();
        invoiceField.setPrefSize(320, 42);
        invoiceField.setStyle(fieldStyle);

        grid.add(productLbl, 0, 0);
        grid.add(productBox, 1, 0);

        grid.add(qtyLbl, 0, 1);
        grid.add(qtyField, 1, 1);

        grid.add(invoiceLbl, 0, 2);
        grid.add(invoiceField, 1, 2);

        // ================= RIGHT SIDE =================
        Label statusLbl = new Label("Status");
        statusLbl.setFont(labelFont);
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Sent", "Received");
        statusBox.setPrefSize(320, 42);
        statusBox.setStyle(fieldStyle);

        Label dateLbl = new Label("RTS Date");
        dateLbl.setFont(labelFont);
        rtsDate.setPrefSize(320, 42);
        rtsDate.setStyle(fieldStyle);

        grid.add(statusLbl, 2, 0);
        grid.add(statusBox, 3, 0);

        grid.add(dateLbl, 2, 1);
        grid.add(rtsDate, 3, 1);

        // ================= BUTTONS =================
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

        HBox buttonBox = new HBox(40);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(saveBtn, backBtn);

        grid.add(buttonBox, 2, 3, 2, 1);

        backBtn.setOnAction(e -> app.showDashboard());

        // ================= DATABASE LOGIC =================
        saveBtn.setOnAction(e -> {

            if (productBox.getValue() == null || qtyField.getText().isEmpty() ||
                    invoiceField.getText().isEmpty() || statusBox.getValue() == null ||
                    rtsDate.getValue() == null) {
                new Alert(Alert.AlertType.ERROR, "All fields are mandatory").show();
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(qtyField.getText());
                if (quantity <= 0) throw new Exception();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Enter valid quantity").show();
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                // Insert into RTS table
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO rts_table (bill_id, pid, sid, quantity, rts_date, status, invoice_no) " +
                                "VALUES ( (SELECT bill_id FROM bill_invoice WHERE pid=? LIMIT 1), ?, " +
                                "(SELECT sid FROM supplies WHERE pid=? LIMIT 1), ?, ?, ?, ?)");

                ps.setString(1, productBox.getValue().getPid());
                ps.setString(2, productBox.getValue().getPid());
                ps.setInt(3, quantity);
                ps.setDate(4, Date.valueOf(rtsDate.getValue()));
                ps.setString(5, statusBox.getValue());
                ps.setString(6, invoiceField.getText());
                ps.executeUpdate();

                // Update product stock
                PreparedStatement update = con.prepareStatement(
                        "UPDATE product SET qty_in_stock = qty_in_stock - ? WHERE pid=?");
                update.setInt(1, quantity);
                update.setString(2, productBox.getValue().getPid());
                update.executeUpdate();

                con.commit();
                new Alert(Alert.AlertType.INFORMATION, "RTS saved successfully").show();

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error in RTS").show();
                ex.printStackTrace();
            }
        });

        VBox root = new VBox();
        root.setPadding(new Insets(50));
        root.setSpacing(35);
        root.setStyle("-fx-background-color: #E2C49F;");

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


        root.getChildren().add(card);

        return root;
    }

    private void loadProducts(ComboBox<Product> box) {

        ObservableList<Product> list = FXCollections.observableArrayList();

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs =
                     st.executeQuery("SELECT pid, product_name, qty_in_stock FROM product")) {

            while (rs.next()) {
                list.add(new Product(
                        rs.getString("pid"),
                        rs.getString("product_name"),
                        rs.getInt("qty_in_stock")
                ));
            }

        } catch (Exception e) { }

        box.setItems(list);
    }
}