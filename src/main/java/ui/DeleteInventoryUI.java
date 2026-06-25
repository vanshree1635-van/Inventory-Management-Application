package ui;

import com.mycompany.inventory.Inventory;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.*;
import java.time.LocalDate;

public class DeleteInventoryUI {

    private final Scene scene;

    public DeleteInventoryUI(Inventory app) {

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(60));
        grid.setHgap(50);
        grid.setVgap(40);
        grid.setStyle("-fx-background-color:#d2b48c;");

        Font labelFont = Font.font("Arial", FontWeight.BOLD, 22);

        Label typeLabel = new Label("Product Type");
        Label nameLabel = new Label("Product Name");
        Label supplierLabel = new Label("Supplier Name");
        Label dateLabel = new Label("Purchased Date");
        Label billLabel = new Label("Invoice");

        typeLabel.setFont(labelFont);
        nameLabel.setFont(labelFont);
        supplierLabel.setFont(labelFont);
        dateLabel.setFont(labelFont);
        billLabel.setFont(labelFont);

        ComboBox<String> typeBox = new ComboBox<>();
        ComboBox<String> nameBox = new ComboBox<>();
        ComboBox<String> supplierBox = new ComboBox<>();
        ComboBox<LocalDate> dateBox = new ComboBox<>();
        ComboBox<Integer> billBox = new ComboBox<>();

        Control[] controls = { typeBox, nameBox, supplierBox, dateBox, billBox };
        for (Control c : controls) {
            c.setPrefWidth(450);
            c.setPrefHeight(50);
            c.setStyle("-fx-font-size:18px;");
        }

        grid.add(typeLabel, 0, 0);   grid.add(typeBox, 1, 0);
        grid.add(nameLabel, 0, 1);   grid.add(nameBox, 1, 1);
        grid.add(supplierLabel, 0, 2); grid.add(supplierBox, 1, 2);
        grid.add(dateLabel, 0, 3);   grid.add(dateBox, 1, 3);
        grid.add(billLabel, 0, 4);   grid.add(billBox, 1, 4);

        Button deleteBtn = new Button("Delete");
        deleteBtn.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        deleteBtn.setPrefSize(150, 45);

        Button backBtn = new Button("Back");
        backBtn.setPrefSize(120, 40);
        backBtn.setOnAction(e -> app.showDashboard());

        grid.add(deleteBtn, 1, 6);
        grid.add(backBtn, 1, 7);

        String DB_URL = "jdbc:mysql://localhost:3306/inventory_system";
        String DB_USER = "root";
        String DB_PASS = "Somya@2005";

        // ================= PRODUCT TYPES =================
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             ResultSet rs = con.createStatement()
                     .executeQuery("SELECT ptype_name FROM product_type")) {

            while (rs.next()) typeBox.getItems().add(rs.getString(1));
        } catch (Exception ex) {
            showError(ex);
        }

        // ================= TYPE → PRODUCTS =================
        typeBox.setOnAction(e -> {
            nameBox.getItems().clear();
            supplierBox.getItems().clear();
            dateBox.getItems().clear();
            billBox.getItems().clear();

            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT DISTINCT p.product_name " +
                         "FROM product p JOIN product_type pt ON p.ptype_id=pt.ptype_id " +
                         "WHERE pt.ptype_name=? AND p.status='ACTIVE'")) {

                ps.setString(1, typeBox.getValue());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) nameBox.getItems().add(rs.getString(1));
            } catch (Exception ex) {
                showError(ex);
            }
        });

        // ================= PRODUCT → SUPPLIERS =================
        nameBox.setOnAction(e -> {
            supplierBox.getItems().clear();
            dateBox.getItems().clear();
            billBox.getItems().clear();

            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT DISTINCT s.name " +
                         "FROM supplier s " +
                         "JOIN bill_invoice bi ON s.sid=bi.sid " +
                         "JOIN product p ON p.pid=bi.pid " +
                         "WHERE p.product_name=? AND p.status='ACTIVE'")) {

                ps.setString(1, nameBox.getValue());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) supplierBox.getItems().add(rs.getString(1));
            } catch (Exception ex) {
                showError(ex);
            }
        });

        // ================= SUPPLIER → DATES =================
        supplierBox.setOnAction(e -> {
            dateBox.getItems().clear();
            billBox.getItems().clear();

            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT DISTINCT bi.date " +
                         "FROM bill_invoice bi " +
                         "JOIN supplier s ON bi.sid=s.sid " +
                         "JOIN product p ON bi.pid=p.pid " +
                         "WHERE p.product_name=? AND s.name=?")) {

                ps.setString(1, nameBox.getValue());
                ps.setString(2, supplierBox.getValue());
                ResultSet rs = ps.executeQuery();

                while (rs.next())
                    dateBox.getItems().add(rs.getDate(1).toLocalDate());
            } catch (Exception ex) {
                showError(ex);
            }
        });

        // ================= DATE → INVOICE =================
        dateBox.setOnAction(e -> {
    billBox.getItems().clear();

    if (dateBox.getValue() == null ||
        nameBox.getValue() == null ||
        supplierBox.getValue() == null) {
        return; // ⛔ Prevent crash
    }

    try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement ps = con.prepareStatement(
                 "SELECT bill_id FROM bill_invoice bi " +
                 "JOIN supplier s ON bi.sid=s.sid " +
                 "JOIN product p ON bi.pid=p.pid " +
                 "WHERE p.product_name=? AND s.name=? AND bi.date=?")) {

        ps.setString(1, nameBox.getValue());
        ps.setString(2, supplierBox.getValue());
        ps.setDate(3, Date.valueOf(dateBox.getValue()));

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            billBox.getItems().add(rs.getInt(1));
        }
    } catch (Exception ex) {
        showError(ex);
    }
});
        // ================= DELETE =================
        deleteBtn.setOnAction(e -> {
            if (billBox.getValue() == null) {
                showError(new Exception("Please select an invoice"));
                return;
            }

            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

                con.setAutoCommit(false);

                int pid;

                // 1️⃣ Get product id
                try (PreparedStatement ps1 = con.prepareStatement(
                        "SELECT pid FROM bill_invoice WHERE bill_id=?")) {

                    ps1.setInt(1, billBox.getValue());
                    ResultSet rs = ps1.executeQuery();

                    if (!rs.next()) throw new Exception("Invoice not found");
                    pid = rs.getInt(1);
                }

                // 2️⃣ Delete invoice
                try (PreparedStatement ps2 = con.prepareStatement(
                        "DELETE FROM bill_invoice WHERE bill_id=?")) {

                    ps2.setInt(1, billBox.getValue());
                    ps2.executeUpdate();
                }

                // 3️⃣ Check remaining invoices
                boolean hasInvoices;
                try (PreparedStatement ps3 = con.prepareStatement(
                        "SELECT COUNT(*) FROM bill_invoice WHERE pid=?")) {

                    ps3.setInt(1, pid);
                    ResultSet rs = ps3.executeQuery();
                    rs.next();
                    hasInvoices = rs.getInt(1) > 0;
                }

                // 4️⃣ Mark product inactive if no invoices left
                if (!hasInvoices) {
                    try (PreparedStatement ps4 = con.prepareStatement(
                            "UPDATE product SET status='INACTIVE' WHERE pid=?")) {

                        ps4.setInt(1, pid);
                        ps4.executeUpdate();
                    }
                }

                con.commit();

                new Alert(Alert.AlertType.INFORMATION,
                        "Invoice deleted successfully").show();

                billBox.getItems().clear();
                dateBox.getItems().clear();
                supplierBox.getItems().clear();
                nameBox.getItems().clear();

            } catch (Exception ex) {
                showError(ex);
            }
        });

        scene = new Scene(grid, 1024, 768);
    }

    private void showError(Exception ex) {
        new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
    }

    public Scene getScene() {
        return scene;
    }
}