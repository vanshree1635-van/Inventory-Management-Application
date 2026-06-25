package ui;

import com.mycompany.inventory.Inventory;
import db.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Product;

import java.sql.*;
import java.time.LocalDate;

public class IssueUI {

    private final Scene scene;

    public IssueUI(Inventory app) {

        // 🔒 ROLE CHECK
        if (!"Manager".equalsIgnoreCase(Inventory.getUserRole())) {
            new Alert(Alert.AlertType.ERROR,
                    "Access Denied: Only Manager can issue inventory").show();
            app.showDashboard();
        }

        GridPane grid = new GridPane();

        Label formTitle = new Label("ISSUE INVENTORY FORM");
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

        // ================= LEFT SIDE =================

        Label productLbl = new Label("Product Name");
        productLbl.setFont(labelFont);

        ComboBox<Product> productBox = new ComboBox<>();
        productBox.setPrefSize(320, 42);
        productBox.setStyle(fieldStyle);
        loadProducts(productBox);

        Label issueToLbl = new Label("Issued To");
        issueToLbl.setFont(labelFont);

        TextField issueTo = new TextField();
        issueTo.setPrefSize(320, 42);
        issueTo.setStyle(fieldStyle);

        Label deptLbl = new Label("Dept Name");
        deptLbl.setFont(labelFont);

        // ✅ DEFAULT VALUE SET HERE
        TextField dept = new TextField("Computer Science");
        dept.setPrefSize(320, 42);
        dept.setStyle(fieldStyle);
        dept.setEditable(true); // Manager can overwrite

        Label qtyLbl = new Label("Quantity Issued");
        qtyLbl.setFont(labelFont);

        TextField qty = new TextField();
        qty.setPrefSize(320, 42);
        qty.setStyle(fieldStyle);

        grid.add(productLbl, 0, 0);
        grid.add(productBox, 1, 0);

        grid.add(issueToLbl, 0, 1);
        grid.add(issueTo, 1, 1);

        grid.add(deptLbl, 0, 2);
        grid.add(dept, 1, 2);

        grid.add(qtyLbl, 0, 3);
        grid.add(qty, 1, 3);

        // ================= RIGHT SIDE =================

        Label reasonLbl = new Label("Reason");
        reasonLbl.setFont(labelFont);

        TextField reason = new TextField();
        reason.setPrefSize(320, 42);
        reason.setStyle(fieldStyle);

        Label dateLbl = new Label("Issued Date");
        dateLbl.setFont(labelFont);

        DatePicker date = new DatePicker(LocalDate.now());
        date.setPrefSize(320, 42);
        date.setStyle(fieldStyle);

        Label issuedByLbl = new Label("Issued By");
        issuedByLbl.setFont(labelFont);

        ComboBox<String> issuedBy = new ComboBox<>();
        issuedBy.getItems().addAll("M01", "M02");
        issuedBy.setPrefSize(320, 42);
        issuedBy.setStyle(fieldStyle);

        grid.add(reasonLbl, 2, 0);
        grid.add(reason, 3, 0);

        grid.add(dateLbl, 2, 1);
        grid.add(date, 3, 1);

        grid.add(issuedByLbl, 2, 2);
        grid.add(issuedBy, 3, 2);

        // ================= BUTTONS =================

        Button issueBtn = new Button("SAVE");
        Button back = new Button("BACK");

        String buttonStyle = """
            -fx-background-color: #8B5E34;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-padding: 12 35 12 35;
        """;

        issueBtn.setStyle(buttonStyle);
        back.setStyle(buttonStyle);
        issueBtn.setPrefSize(180, 50);
        back.setPrefSize(180, 50);

        HBox buttonBox = new HBox(40);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(issueBtn, back);

        grid.add(buttonBox, 2, 6, 2, 1);

        back.setOnAction(e -> app.showDashboard());

        // ================= DATABASE LOGIC =================

        issueBtn.setOnAction(e -> {

            if (productBox.getValue() == null ||
                    issueTo.getText().isEmpty() ||
                    dept.getText().isEmpty() ||
                    qty.getText().isEmpty() ||
                    issuedBy.getValue() == null ||
                    date.getValue() == null) {

                new Alert(Alert.AlertType.ERROR,
                        "All fields are mandatory").show();
                return;
            }

            int quantity;

            try {
                quantity = Integer.parseInt(qty.getText());
                if (quantity <= 0) throw new Exception();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Enter valid quantity").show();
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                PreparedStatement check = con.prepareStatement(
                        "SELECT qty_in_stock FROM product WHERE pid=?");
                check.setString(1, productBox.getValue().getPid());
                ResultSet rs = check.executeQuery();

                rs.next();
                int stock = rs.getInt("qty_in_stock");

                if (quantity > stock) {
                    new Alert(Alert.AlertType.ERROR,
                            "Insufficient stock! Available: " + stock).show();
                    con.rollback();
                    return;
                }

                PreparedStatement insert = con.prepareStatement(
                        "INSERT INTO issue " +
                                "(pid, issue_to, issued_by, dept_name, qty_issued, reason, date) " +
                                "VALUES (?,?,?,?,?,?,?)");

                insert.setString(1, productBox.getValue().getPid());
                insert.setString(2, issueTo.getText());
                insert.setString(3, issuedBy.getValue());
                insert.setString(4, dept.getText());
                insert.setInt(5, quantity);
                insert.setString(6, reason.getText());
                insert.setDate(7, Date.valueOf(date.getValue()));

                insert.executeUpdate();

                PreparedStatement update = con.prepareStatement(
                        "UPDATE product SET qty_in_stock = qty_in_stock - ? WHERE pid=?");

                update.setInt(1, quantity);
                update.setString(2, productBox.getValue().getPid());
                update.executeUpdate();

                con.commit();

                new Alert(Alert.AlertType.INFORMATION,
                        "Product Issued Successfully").show();

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Database Error").show();
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

        } catch (Exception e) {
            e.printStackTrace();
        }

        box.setItems(list);
    }
}