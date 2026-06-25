package ui;

import com.mycompany.inventory.Inventory;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.ProductRow;

import java.sql.*;

public class ModifyProductUI {

    private Scene scene;

    private final String DB_URL = "jdbc:mysql://127.0.0.1:3306/inventory_system";
    private final String DB_USER = "root";
    private final String DB_PASS = "Somya@2005";

    public ModifyProductUI(Inventory app) {

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color:#d2b48c;");

        Label title = new Label("Modify Products");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));

        TableView<ProductRow> table = new TableView<>();

        TableColumn<ProductRow, String> idCol = new TableColumn<>("PID");
        idCol.setCellValueFactory(d -> d.getValue().pidProperty());

        TableColumn<ProductRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());

        TableColumn<ProductRow, Number> qtyCol = new TableColumn<>("Stock");
        qtyCol.setCellValueFactory(d -> d.getValue().qtyProperty());

        TableColumn<ProductRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> d.getValue().ptypeProperty());

        // 🔥 EDIT BUTTON COLUMN
        TableColumn<ProductRow, Void> editCol = new TableColumn<>("Edit");

        editCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Edit");

            {
                btn.setOnAction(e -> {
                    ProductRow row = getTableView().getItems().get(getIndex());
                    openEditDialog(row, table);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(idCol, nameCol, qtyCol, typeCol, editCol);

        loadProducts(table);

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> app.showDashboard());

        root.getChildren().addAll(title, table, backBtn);

        scene = new Scene(root, 900, 600);
    }

    // ================= LOAD DATA =================
    private void loadProducts(TableView<ProductRow> table) {

        table.getItems().clear();

        String sql = "SELECT pid, product_name, qty_in_stock, ptype_id FROM product";

        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                table.getItems().add(new ProductRow(
                        rs.getString("pid"),
                        rs.getString("product_name"),
                        rs.getInt("qty_in_stock"),
                        rs.getString("ptype_id")
                ));
            }

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    // ================= EDIT POPUP =================
    private void openEditDialog(ProductRow row, TableView<ProductRow> table) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Product");

        TextField nameField = new TextField(row.getName());
        TextField qtyField = new TextField(String.valueOf(row.getQty()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Quantity:"), 0, 1);
        grid.add(qtyField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {
                try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     PreparedStatement ps = con.prepareStatement(
                             "UPDATE product SET product_name=?, qty_in_stock=? WHERE pid=?")) {

                    ps.setString(1, nameField.getText());
                    ps.setInt(2, Integer.parseInt(qtyField.getText()));
                    ps.setString(3, row.getPid());

                    ps.executeUpdate();

                    new Alert(Alert.AlertType.INFORMATION, "Updated Successfully").show();

                    loadProducts(table);

                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
                }
            }
        });
    }

    public Scene getScene() {
        return scene;
    }
}