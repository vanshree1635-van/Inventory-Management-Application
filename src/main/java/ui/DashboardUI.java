package ui;

import com.mycompany.inventory.Inventory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.beans.binding.Bindings;
import service.NotificationService;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class DashboardUI {

    private final Scene scene;

    // ---------------------- LOW INVENTORY ALERT ----------------------
    private void showLowInventoryAlert(String itemName, int quantity) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Low Inventory Alert");
        alert.setHeaderText("Low Stock Detected");
        alert.setContentText(
                "Item: " + itemName + "\n" +
                "Available Quantity: " + quantity + "\n\n" +
                "Please restock this item."
        );
        alert.showAndWait();
    }

    private void checkLowInventoryFromUser() {
        TextInputDialog itemDialog = new TextInputDialog();
        itemDialog.setTitle("Inventory Input");
        itemDialog.setHeaderText("Enter Item Name");
        itemDialog.setContentText("Item:");
        Optional<String> itemResult = itemDialog.showAndWait();
        if (!itemResult.isPresent()) return;
        String itemName = itemResult.get();

        TextInputDialog qtyDialog = new TextInputDialog();
        qtyDialog.setTitle("Inventory Input");
        qtyDialog.setHeaderText("Enter Available Quantity");
        qtyDialog.setContentText("Quantity:");
        Optional<String> qtyResult = qtyDialog.showAndWait();
        if (!qtyResult.isPresent()) return;

        int quantity;
        try {
            quantity = Integer.parseInt(qtyResult.get());
        } catch (NumberFormatException e) {
            return;
        }

        if (quantity < 10) {
            showLowInventoryAlert(itemName, quantity);
        }
    }

    // ---------------------- REPORT ALERT ----------------------
    private void showReportAlert(String type) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Report");
        alert.setHeaderText(type + " Report");
        alert.setContentText(type + " report selected.");
        alert.showAndWait();
    }

    // ---------------------- CONSTRUCTOR ----------------------
    public DashboardUI(Inventory app) {

        // ===== TOP BROWN BAR =====
        Label userManual = new Label("User Manual");
        userManual.setOnMouseClicked(e -> openUserManual());
        userManual.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        Label logout = new Label("Logout");
        logout.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        logout.setOnMouseClicked((MouseEvent e) -> app.showLogin());

        Label greetingLabel = new Label(getGreeting() + "   |   " + getIndianDateTime());
        greetingLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        String user = Inventory.getLoggedUser();

        String firstLetter = user.substring(0,1).toUpperCase();

        Label letter = new Label(firstLetter);
        letter.setStyle(
                "-fx-text-fill:white;" +
                "-fx-font-size:14;" +
                "-fx-font-weight:bold;"
        );

        Circle circle = new Circle(14);
        circle.setFill(Color.web("#8B5E34"));

        StackPane profile = new StackPane(circle, letter);
        profile.setPrefSize(28,28);

        ContextMenu profileMenu = new ContextMenu();

        profile.setOnMouseClicked(e ->
                profileMenu.show(profile, Side.BOTTOM, 0, 0)
        );
        StackPane profileIcon = new StackPane(profile);

        MenuItem userItem = new MenuItem("User : " + Inventory.getLoggedUser());
        MenuItem roleItem = new MenuItem("Role : " + Inventory.getUserRole());
        MenuItem notificationItem = new MenuItem("Notifications");

        profileMenu.getItems().addAll(userItem, roleItem, notificationItem);

        notificationItem.setOnAction(e -> {

            Stage stage = new Stage();

            VBox box = new VBox(15);
            box.setPadding(new Insets(25));
            box.setAlignment(Pos.TOP_LEFT);

            box.setStyle(
                "-fx-background-color:#F5DEB3;" +
                "-fx-border-color:#8B5E34;" +
                "-fx-border-width:2;" +
                "-fx-background-radius:8;"
            );

            if(NotificationService.getNotifications().isEmpty()){
                Label empty = new Label("No Notifications Yet");
                empty.setStyle(
                    "-fx-font-size:14;" +
                    "-fx-text-fill:#3E2723;" +
                    "-fx-font-weight:bold;"
                );
                box.getChildren().add(empty);
            }
            for(String msg : NotificationService.getNotifications()) {
                Label lbl = new Label(msg);
                lbl.setWrapText(true);
                lbl.setStyle(
                    "-fx-font-size:14;" +
                    "-fx-text-fill:#3E2723;" +
                    "-fx-font-weight:bold;"
                );
                box.getChildren().add(lbl);
            }

            ScrollPane scroll = new ScrollPane(box);
            scroll.setFitToWidth(true);
            scroll.setStyle(
                "-fx-background:#F5DEB3;" +
                "-fx-border-color:#8B5E34;"
            );
            scroll.setPadding(new Insets(10));

            Scene scene = new Scene(scroll,350,250);
            stage.setScene(scene);
            stage.setTitle("Notifications");
            stage.show();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(25, greetingLabel, spacer, profileIcon, userManual, logout);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(12, 30, 12, 30));
        topBar.setStyle(
            "-fx-background-color:#F5DEB3;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0.2, 0, 3);"
        );

        // ===== BUTTON GRID =====
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(35);
        grid.setVgap(35);
        grid.setPadding(new Insets(60));
        StackPane.setAlignment(grid, Pos.CENTER);

        String[] names = { "Update", "Issue", "Return", "View", "Report",
                "Budget Analysis", "Request Inventory" };

        int index = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (index >= names.length) break;

                Button btn = new Button(names[index++]);
                btn.setFont(Font.font("Arial", FontWeight.NORMAL, 30));
                btn.setPrefSize(320, 120);
                String btnStyle =
                    "-fx-background-color:#F5DEB3;" +
                    "-fx-border-color:#b08968;" +
                    "-fx-border-width:2;" +
                    "-fx-background-radius:10;" +
                    "-fx-font-size:22px;" +
                    "-fx-font-weight:bold;" +
                    "-fx-text-fill:#2E2E2E;";
                btn.setStyle(btnStyle);
                btn.setOnMouseEntered(e ->
                    btn.setStyle(btnStyle + "-fx-background-color:#c19a6b;")
                );
                btn.setOnMouseExited(e ->
                    btn.setStyle(btnStyle)
                );

                String role = Inventory.getUserRole();
                // HOD and Dean restrictions
                if(role.equalsIgnoreCase("HOD") || role.equalsIgnoreCase("Dean")){
                    if(!btn.getText().equals("Report") &&
                       !btn.getText().equals("Budget Analysis")){
                        btn.setDisable(true);
                        btn.setOpacity(0.5);
                    }
                }

                // Manager restriction
                if(role.equalsIgnoreCase("Manager")){
                    if(btn.getText().equals("Budget Analysis")){
                        btn.setDisable(true);
                        btn.setOpacity(0.5);
                    }
                }

                // ---------------- UPDATE DROPDOWN ----------------
                if (btn.getText().equals("Update")) {
                    ContextMenu updateMenu = new ContextMenu();
                    
                  //------------------changes by me    26-----------  
                    Button orderPlacedBtn = new Button("Order Placed");
                    
                    Button addBtn = new Button("Bill Received");
                 
                    Button modifyBtn = new Button("Modify");
                    

                    String bigMenuStyle = "-fx-font-size: 22px;" +
                        "-fx-background-color: #F5DEB3;" +   // same beige as login
                        "-fx-background-radius: 6;" +        // small radius = rectangle
                        "-fx-border-radius: 6;" +
                        "-fx-border-color: #8B5E34;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: #2C3E50;";

                    orderPlacedBtn.setStyle(bigMenuStyle);
                    addBtn.setStyle(bigMenuStyle);
                    modifyBtn.setStyle(bigMenuStyle);

                    orderPlacedBtn.setPrefSize(200, 60);
                    addBtn.setPrefSize(200, 60);
                    modifyBtn.setPrefSize(200, 60);

                    CustomMenuItem addItem = new CustomMenuItem(addBtn, true);
                
                    CustomMenuItem modifyItem = new CustomMenuItem(modifyBtn, false);
                    CustomMenuItem orderItem = new CustomMenuItem(orderPlacedBtn, true);
                    updateMenu.getItems().addAll(addItem, modifyItem, orderItem);

                    btn.setOnAction(e -> updateMenu.show(btn, Side.BOTTOM, 0, 0));

                    orderPlacedBtn.setOnAction(e -> {
                        updateMenu.hide();
                        app.showAddOrder();
                    });
                    
                    addBtn.setOnAction(e -> {
                        updateMenu.hide();
                        app.showAddInventory();
                    });

                    // --------- MODIFY  ----------
                   modifyBtn.setOnAction(e -> {
                        updateMenu.hide();
                        app.showManageInventory();   // NEW SCREEN
                    });
                }
                // ---------------- REPORT DROPDOWN ----------------
                if (btn.getText().equals("Report")) {
                    ContextMenu reportMenu = new ContextMenu();
                    Button weeklyBtn = new Button("Weekly");
                    Button monthlyBtn = new Button("Monthly");
                    Button yearlyBtn = new Button("Yearly");

                    String bigMenuStyle = "-fx-font-size: 22px;" +
                        "-fx-background-color: #F5DEB3;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;" +
                        "-fx-border-color: #8B5E34;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: #2C3E50;";

                    weeklyBtn.setStyle(bigMenuStyle);
                    monthlyBtn.setStyle(bigMenuStyle);
                    yearlyBtn.setStyle(bigMenuStyle);

                    weeklyBtn.setPrefSize(200, 60);
                    monthlyBtn.setPrefSize(200, 60);
                    yearlyBtn.setPrefSize(200, 60);

                    reportMenu.getItems().addAll(
                            new CustomMenuItem(weeklyBtn),
                            new CustomMenuItem(monthlyBtn),
                            new CustomMenuItem(yearlyBtn)
                    );

                    btn.setOnAction(e -> reportMenu.show(btn, Side.BOTTOM, 0, 0));
                    weeklyBtn.setOnAction(e -> {
                        ReportUI report = new ReportUI("Weekly");
                        report.show();
                    });
                    monthlyBtn.setOnAction(e -> {
                        ReportUI report = new ReportUI("Monthly");
                        report.show();
                    });
                    yearlyBtn.setOnAction(e -> {
                        ReportUI report = new ReportUI("Yearly");
                        report.show();
                    });
                }

                // ---------------- BUDGET ANALYSIS ----------------
                if (btn.getText().equals("Budget Analysis")) {
                    btn.setOnAction(e -> {
                        BudgetAnalysisUI ui = new BudgetAnalysisUI();
                        Stage stage = new Stage();
                        stage.setScene(ui.getScene());
                        stage.setTitle("Budget Analysis");
                        stage.show();
                    });
                }

                // ---------------- REQUEST INVENTORY ----------------
                if (btn.getText().equals("Request Inventory")) {
                    btn.setOnAction(e -> showRecipientSelection());
                }

                // ================================================================
                // ---------------- VIEW DROPDOWN (UPDATED) -----------------------
                // Now has three options:
                //   1. "Order Details"         → showOrderTable()
                //   2. "Issue & Return Details" → showIssueReturnTable()
                //   3. "RTS Details"            → showRtsTable()
                // ================================================================
                if (btn.getText().equals("View")) {
                    ContextMenu viewMenu = new ContextMenu();
                    Button orderBtn       = new Button("Order Details");
                    Button issueReturnBtn = new Button("Issue & Return Details");
                    Button rtsBtn         = new Button("RTS Details");

                    String bigMenuStyle = "-fx-font-size: 22px;" +
                        "-fx-background-color: #F5DEB3;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;" +
                        "-fx-border-color: #8B5E34;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: #2C3E50;";

                    orderBtn.setStyle(bigMenuStyle);
                    issueReturnBtn.setStyle(bigMenuStyle);
                    rtsBtn.setStyle(bigMenuStyle);

                    orderBtn.setPrefSize(260, 60);
                    issueReturnBtn.setPrefSize(260, 60);
                    rtsBtn.setPrefSize(260, 60);

                    viewMenu.getItems().addAll(
                            new CustomMenuItem(orderBtn, true),
                            new CustomMenuItem(issueReturnBtn, true),
                            new CustomMenuItem(rtsBtn, true)
                    );

                    btn.setOnAction(e -> viewMenu.show(btn, Side.BOTTOM, 0, 0));

                    orderBtn.setOnAction(e -> {
                        viewMenu.hide();
                        showOrderTable();
                    });
                    issueReturnBtn.setOnAction(e -> {
                        viewMenu.hide();
                        showIssueReturnTable();
                    });
                    rtsBtn.setOnAction(e -> {
                        viewMenu.hide();
                        showRtsTable();
                    });
                }
                // ================================================================

                // ---------------- ISSUE & RETURN ----------------
                if (btn.getText().equals("Issue")) {
                    btn.setOnAction(e -> app.showIssue());
                }
                if (btn.getText().equals("Return")) {

                    ContextMenu returnMenu = new ContextMenu();

                    Button inventoryToSupplier = new Button("Inventory → Supplier");
                    Button issuedReturn = new Button("Issued Person → Inventory");

                    String style = "-fx-font-size: 18px;" +
                            "-fx-background-color:#F5DEB3;" +
                            "-fx-border-color:#8B5E34;" +
                            "-fx-border-width:2;";

                    inventoryToSupplier.setStyle(style);
                    issuedReturn.setStyle(style);

                    inventoryToSupplier.setPrefSize(250, 50);
                    issuedReturn.setPrefSize(250, 50);

                    returnMenu.getItems().addAll(
                        new CustomMenuItem(inventoryToSupplier, true),
                        new CustomMenuItem(issuedReturn, true)
                    );

                    btn.setOnAction(e -> returnMenu.show(btn, Side.BOTTOM, 0, 0));

                    inventoryToSupplier.setOnAction(e -> {
                        returnMenu.hide();
                        app.showReturn("INVENTORY_TO_SUPPLIER");
                    });

                    issuedReturn.setOnAction(e -> {
                        returnMenu.hide();
                        app.showReturn(ReturnUI.ISSUE_TO_INVENTORY);
                    });
                }

                grid.add(btn, c, r);
            }
        }

        // ===== BACKGROUND IMAGE =====
        ImageView bgView = new ImageView(
                new Image(getClass().getResource("/images/pexels-web-buz-29454379.jpg").toExternalForm())
        );

        bgView.setFitWidth(1024);
        bgView.setFitHeight(768);
        bgView.setPreserveRatio(false);

        // ===== BLUR EFFECT =====
        bgView.setEffect(new GaussianBlur(15));

        // ===== DARK OVERLAY =====
        Rectangle darkOverlay = new Rectangle(1024, 768);
        darkOverlay.setFill(Color.rgb(0, 0, 0, 0.5));

        // ===== CENTER STACK =====
        Label deptBox = new Label("Aim & Act Department (Computer Science)");

        deptBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #FFF3E0, #D7B98E);" +
                "-fx-text-fill:#3E2723;" +
                "-fx-font-size:26px;" +
                "-fx-font-weight:bold;" +
                "-fx-padding:10 40 10 40;" +
                "-fx-background-radius:12;" +
                "-fx-border-radius:12;" +
                "-fx-border-color:#8B5E34;" +
                "-fx-border-width:2;" +
                "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.45), 18,0.4,4,4);"
        );

        StackPane.setAlignment(deptBox, Pos.TOP_CENTER);
        deptBox.setTranslateY(25);

        StackPane centerStack = new StackPane(
                bgView,
                darkOverlay,
                deptBox,
                grid
        );

        // ===== ROOT =====
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerStack);

        scene = new Scene(root, 1024, 768);

        // ===== RESPONSIVE =====
        bgView.fitWidthProperty().bind(scene.widthProperty());
        bgView.fitHeightProperty().bind(scene.heightProperty());
        darkOverlay.widthProperty().bind(scene.widthProperty());
        darkOverlay.heightProperty().bind(scene.heightProperty());
    }

    // ---------------------- USER MANUAL ----------------------
    private void openUserManual() {
        try {
            InputStream is = getClass().getResourceAsStream("/USER MANUAL.pdf");
            if (is == null) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("User Manual Not Found");
                alert.setContentText("USER_MANUAL.pdf not found in resources folder.");
                alert.showAndWait();
                return;
            }
            Path tempFile = Files.createTempFile("UserManual", ".pdf");
            Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Desktop.getDesktop().open(tempFile.toFile());
        } catch (IOException e) {
        }
    }

    // ================================================================
    // ---------------------- TABLE VIEWS -----------------------------
    // ================================================================

    /**
     * ORDER DETAILS TABLE
     * Combines: order_table + bill_invoice + product + supplier
     *
     * Columns:
     *   Product Name | Qty Ordered | Order Date | Order Status |
     *   Qty Received | Received Date | Bill No | Bill Amount |
     *   Bill Status  | Supplier Name | Supplier Contact
     */
    private void showOrderTable() {
        Stage stage = new Stage();

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        String[] colNames = {
            "Product Name", "Qty Ordered", "Order Date", "Order Status",
            "Qty Received", "Received Date", "Bill No", "Bill Amount",
            "Bill Status", "Supplier Name", "Supplier Contact"
        };
        int[] colWidths = { 150, 110, 110, 120, 110, 120, 100, 110, 100, 150, 140 };

        for (int i = 0; i < colNames.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(colNames[i]);
            col.setPrefWidth(colWidths[i]);
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    data.getValue()[colIndex] != null ? data.getValue()[colIndex] : ""
                )
            );
            table.getColumns().add(col);
        }

        ObservableList<String[]> data = FXCollections.observableArrayList();

        String query =
            "SELECT p.product_name, " +
            "ot.qty_ordered, " +
            "ot.order_date, " +
            "ot.order_status, " +
            "bi.qty_received, " +
            "bi.received_date, " +
            "bi.bill_no, " +
            "bi.bill_amount, " +
            "bi.bill_status, " +
            "s.name  AS supplier_name, " +
            "s.contact_no " +
            "FROM order_table ot " +
            "JOIN product p       ON ot.pid = p.pid " +
            "JOIN supplier s      ON ot.sid = s.sid " +
            "LEFT JOIN bill_invoice bi " +
            "ON bi.entry_id = ot.entry_id " +
            "AND bi.record_status = 'ACTIVE' " +
            "WHERE ot.record_status = 'ACTIVE' " +
            "ORDER BY ot.order_date DESC";

        try (Connection con = db.DBConnection.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(query)) {

            while (rs.next()) {
                String[] row = new String[11];
                row[0]  = rs.getString("product_name");
                row[1]  = String.valueOf(rs.getInt("qty_ordered"));
                row[2]  = rs.getString("order_date");
                row[3]  = rs.getString("order_status");
                row[4]  = rs.getObject("qty_received") != null
                              ? String.valueOf(rs.getInt("qty_received")) : "—";
                row[5]  = rs.getString("received_date") != null
                              ? rs.getString("received_date") : "—";
                row[6]  = rs.getString("bill_no") != null
                              ? rs.getString("bill_no") : "—";
                row[7]  = rs.getObject("bill_amount") != null
                              ? "Rs. " + rs.getBigDecimal("bill_amount").toPlainString() : "—";
                row[8]  = rs.getString("bill_status") != null
                              ? rs.getString("bill_status") : "—";
                row[9]  = rs.getString("supplier_name");
                row[10] = rs.getString("contact_no");
                data.add(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        table.setItems(data);

        Label title = new Label("Order Details");
        title.setStyle(
            "-fx-font-size:20px; -fx-font-weight:bold;" +
            "-fx-text-fill:#3E2723; -fx-padding:10 0 6 4;"
        );

        VBox root = new VBox(8, title, table);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color:#F5DEB3;");

        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background:#F5DEB3;");

        VBox layout = new VBox(8, title, scroll);
        layout.setPadding(new Insets(14));
        layout.setStyle("-fx-background-color:#F5DEB3;");

        stage.setScene(new Scene(layout, 1150, 480));
        stage.setTitle("Order Details");
        stage.show();
    }

    /**
     * ISSUE & RETURN DETAILS TABLE
     * Combines: issue + return_table + product
     * A LEFT JOIN ensures issues with no return still appear,
     * with "Not Returned" shown in the return columns.
     *
     * Columns:
     *   Product Name | Issued To | Issued By | Department |
     *   Qty Issued   | Qty Returned | Issue Date | Return Date | Return To
     */
    private void showIssueReturnTable() {
        Stage stage = new Stage();

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        String[] colNames = {
            "Product Name", "Issued To", "Issued By", "Department",
            "Qty Issued", "Qty Returned", "Issue Date", "Return Date", "Return To"
        };
        int[] colWidths = { 150, 130, 120, 130, 100, 110, 110, 110, 130 };

        for (int i = 0; i < colNames.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(colNames[i]);
            col.setPrefWidth(colWidths[i]);
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    data.getValue()[colIndex] != null ? data.getValue()[colIndex] : ""
                )
            );
            table.getColumns().add(col);
        }

        ObservableList<String[]> data = FXCollections.observableArrayList();

        // LEFT JOIN so issues without a matching return_table row still appear
        String query =
            "SELECT p.product_name, " +
            "       i.issue_to, " +
            "       i.issued_by, " +
            "       i.dept_name, " +
            "       i.qty_issued, " +
            "       i.qty_returned, " +
            "       i.date       AS issue_date, " +
            "       rt.date      AS return_date, " +
            "       rt.return_to " +
            "FROM issue i " +
            "JOIN product p       ON i.pid = p.pid " +
            "LEFT JOIN return_table rt ON rt.issue_id = i.issue_id " +
            "ORDER BY i.date DESC";

        try (Connection con = db.DBConnection.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(query)) {

            while (rs.next()) {
                String[] row = new String[9];
                row[0] = rs.getString("product_name");
                row[1] = rs.getString("issue_to");
                row[2] = rs.getString("issued_by");
                row[3] = rs.getString("dept_name");
                row[4] = String.valueOf(rs.getInt("qty_issued"));
                row[5] = String.valueOf(rs.getInt("qty_returned"));
                row[6] = rs.getString("issue_date");
                row[7] = rs.getString("return_date") != null
                             ? rs.getString("return_date") : "Not Returned";
                row[8] = rs.getString("return_to") != null
                             ? rs.getString("return_to") : "—";
                data.add(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        table.setItems(data);

        Label title = new Label("Issue & Return Details");
        title.setStyle(
            "-fx-font-size:20px; -fx-font-weight:bold;" +
            "-fx-text-fill:#3E2723; -fx-padding:10 0 6 4;"
        );

        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background:#F5DEB3;");

        VBox layout = new VBox(8, title, scroll);
        layout.setPadding(new Insets(14));
        layout.setStyle("-fx-background-color:#F5DEB3;");

        stage.setScene(new Scene(layout, 1020, 450));
        stage.setTitle("Issue & Return Details");
        stage.show();
    }

    /**
     * RTS (Return To Supplier) DETAILS TABLE
     * Combines: rts_table + product + supplier + bill_invoice
     *
     * Columns:
     *   RTS ID | Product Name | Supplier Name | Supplier Contact |
     *   Bill No | Quantity | RTS Date | Status
     */
    private void showRtsTable() {
        Stage stage = new Stage();

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        String[] colNames = {
            "RTS ID", "Product Name", "Supplier Name", "Supplier Contact",
            "Bill No", "Quantity", "RTS Date", "Status"
        };
        int[] colWidths = { 80, 160, 160, 150, 120, 90, 110, 110 };

        for (int i = 0; i < colNames.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(colNames[i]);
            col.setPrefWidth(colWidths[i]);
            col.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                    data.getValue()[colIndex] != null ? data.getValue()[colIndex] : ""
                )
            );
            table.getColumns().add(col);
        }

        ObservableList<String[]> data = FXCollections.observableArrayList();

        String query =
            "SELECT r.rts_id, " +
            "       p.product_name, " +
            "       s.name        AS supplier_name, " +
            "       s.contact_no, " +
            "       bi.bill_no, " +
            "       r.quantity, " +
            "       r.rts_date, " +
            "       r.record_status " +
            "FROM rts_table r " +
            "JOIN product p       ON r.pid     = p.pid " +
            "JOIN supplier s      ON r.sid     = s.sid " +
            "JOIN bill_invoice bi ON r.bill_id = bi.bill_id " +
            "ORDER BY r.rts_date DESC";

        try (Connection con = db.DBConnection.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(query)) {

            while (rs.next()) {
                String[] row = new String[8];
                row[0] = String.valueOf(rs.getInt("rts_id"));
                row[1] = rs.getString("product_name");
                row[2] = rs.getString("supplier_name");
                row[3] = rs.getString("contact_no");
                row[4] = rs.getString("bill_no") != null
                             ? rs.getString("bill_no") : "—";
                row[5] = String.valueOf(rs.getInt("quantity"));
                row[6] = rs.getString("rts_date");
                row[7] = rs.getString("record_status");
                data.add(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        table.setItems(data);

        Label title = new Label("RTS (Return To Supplier) Details");
        title.setStyle(
            "-fx-font-size:20px; -fx-font-weight:bold;" +
            "-fx-text-fill:#3E2723; -fx-padding:10 0 6 4;"
        );

        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background:#F5DEB3;");

        VBox layout = new VBox(8, title, scroll);
        layout.setPadding(new Insets(14));
        layout.setStyle("-fx-background-color:#F5DEB3;");

        stage.setScene(new Scene(layout, 1000, 450));
        stage.setTitle("RTS Details");
        stage.show();
    }

    // ================================================================
    // ---------------------- BUDGET ANALYSIS -------------------------
    // ================================================================
    
    private void showBudgetAnalysis() {

        Stage stage = new Stage();
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#F5DEB3;");

        Label title = new Label("Budget Analysis Report");
        title.setStyle("-fx-font-size:38px; -fx-font-weight:bold; -fx-text-fill:black;");
        title.setPadding(new Insets(30,0,20,40));

        double totalBudget = 250000;
        double furniture = 80000;
        double stationery = 60000;
        double miscellaneous = 50000;
        double computer = 60000;
        double spent = 150000;
        double remaining = totalBudget - spent;

        HBox cards = new HBox(40);
        cards.setAlignment(Pos.CENTER);
        cards.getChildren().addAll(
                createCard("Total Budget", totalBudget, "#0D47A1"),
                createCard("Amount Spent", spent, "#E65100"),
                createCard("Remaining Balance", remaining, "#1B5E20")
        );

        PieChart pieChart = new PieChart();
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(false);

        PieChart.Data d1 = new PieChart.Data("Furniture", furniture);
        PieChart.Data d2 = new PieChart.Data("Stationery", stationery);
        PieChart.Data d3 = new PieChart.Data("Miscellaneous", miscellaneous);
        PieChart.Data d4 = new PieChart.Data("Computer", computer);

        pieChart.getData().addAll(d1,d2,d3,d4);
        pieChart.setPrefSize(400,400);

        d1.getNode().setStyle("-fx-pie-color:#0D47A1;");
        d2.getNode().setStyle("-fx-pie-color:#E65100;");
        d3.getNode().setStyle("-fx-pie-color:#6A1B9A;");
        d4.getNode().setStyle("-fx-pie-color:#1B5E20;");

        StackPane donutPane = new StackPane();
        Circle hole = new Circle(90, Color.web("#F5DEB3"));
        Label centerAmount = new Label("₹ " + totalBudget);
        centerAmount.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:black;");
        donutPane.getChildren().addAll(pieChart, hole, centerAmount);

        VBox pieLegend = new VBox(15);
        pieLegend.setAlignment(Pos.CENTER_LEFT);
        pieLegend.getChildren().addAll(
                createLegendItem("#0D47A1","Furniture",furniture),
                createLegendItem("#E65100","Stationery",stationery),
                createLegendItem("#6A1B9A","Miscellaneous",miscellaneous),
                createLegendItem("#1B5E20","Computer Sets",computer)
        );

        HBox pieSection = new HBox(40, donutPane, pieLegend);
        pieSection.setAlignment(Pos.CENTER);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Category");
        yAxis.setLabel("Amount");
        xAxis.setTickLabelFill(Color.BLACK);
        yAxis.setTickLabelFill(Color.BLACK);

        BarChart<String,Number> barChart = new BarChart<>(xAxis,yAxis);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(25);
        barChart.setBarGap(5);
        barChart.setPrefSize(500,400);

        XYChart.Series<String,Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Furniture", furniture));
        series.getData().add(new XYChart.Data<>("Stationery", stationery));
        series.getData().add(new XYChart.Data<>("Miscellaneous", miscellaneous));
        series.getData().add(new XYChart.Data<>("Computer", computer));
        barChart.getData().add(series);

        for (XYChart.Data<String,Number> data : series.getData()) {
            data.nodeProperty().addListener((obs,node1,node2)->{
                if(data.getXValue().equals("Furniture"))
                    node2.setStyle("-fx-bar-fill:#0D47A1;");
                else if(data.getXValue().equals("Stationery"))
                    node2.setStyle("-fx-bar-fill:#E65100;");
                else if(data.getXValue().equals("Miscellaneous"))
                    node2.setStyle("-fx-bar-fill:#6A1B9A;");
                else
                    node2.setStyle("-fx-bar-fill:#1B5E20;");
            });
        }

        HBox barLegend = new HBox(40);
        barLegend.setAlignment(Pos.CENTER);
        barLegend.getChildren().addAll(
                createLegendCircle("#0D47A1","Furniture"),
                createLegendCircle("#E65100","Stationery"),
                createLegendCircle("#6A1B9A","Miscellaneous"),
                createLegendCircle("#1B5E20","Computer Sets")
        );

        VBox barSection = new VBox(20, barChart, barLegend);
        barSection.setAlignment(Pos.CENTER);

        HBox center = new HBox(80, pieSection, barSection);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));

        root.setTop(title);
        root.setCenter(new VBox(20, cards, center));

        Scene scene = new Scene(root, 1200, 750);
        stage.setScene(scene);
        stage.setTitle("Budget Analysis Report");
        stage.show();
    }

    private VBox createCard(String title, double amount, String color){
        Label t = new Label(title);
        t.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:white;");
        Label amt = new Label("₹ " + amount);
        amt.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:white;");

        VBox box = new VBox(10,t,amt);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color:"+color+"; -fx-background-radius:15;");
        box.setPrefWidth(250);

        box.setOnMouseEntered(e -> { box.setScaleX(1.05); box.setScaleY(1.05); });
        box.setOnMouseExited(e ->  { box.setScaleX(1);    box.setScaleY(1);    });

        return box;
    }

    private HBox createLegendItem(String color, String name, double amount){
        Circle c = new Circle(8, Color.web(color));
        Label label = new Label(name + "  ₹ " + amount);
        label.setStyle("-fx-font-weight:bold; -fx-text-fill:black;");
        return new HBox(10, c, label);
    }

    private HBox createLegendCircle(String color, String name){
        Circle c = new Circle(8, Color.web(color));
        Label label = new Label(name);
        label.setStyle("-fx-font-weight:bold; -fx-text-fill:black;");
        return new HBox(10, c, label);
    }

    private void showInfoPanel(String title, String message) {
        Label lbl = new Label(message);
        lbl.setWrapText(true);

        VBox box = new VBox(lbl);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:#e7f3ff;" +
                "-fx-border-color:#2196f3;" +
                "-fx-border-radius:10;");

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(box, 350, 200));
        stage.show();
    }

    // ---------------------- GREETING ----------------------
    private String getGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }
        String role = Inventory.getUserRole();
        if (role == null) role = "User";
        return greeting + ", " + role;
    }
    private String getIndianDateTime() {

        ZonedDateTime indiaTime =
                ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  hh:mm a");

        return indiaTime.format(formatter);
    }

    // ---------------------- REQUEST INVENTORY ----------------------

    private void showRecipientSelection() {

        Stage stage = new Stage();

        Label title = new Label("Select Recipient");
        title.setStyle("-fx-font-size:20; -fx-font-weight:bold; -fx-text-fill:#3E2723;");

        Button hodBtn = new Button("Send to HOD");
        Button deanBtn = new Button("Send to Dean");

        String style =
                "-fx-background-color:#8B5E34;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:16;" +
                "-fx-background-radius:8;";

        hodBtn.setStyle(style);
        deanBtn.setStyle(style);

        hodBtn.setPrefWidth(160);
        deanBtn.setPrefWidth(160);

        hodBtn.setOnAction(e -> {
            stage.close();
            showProductForm("HOD");
        });

        deanBtn.setOnAction(e -> {
            stage.close();
            showProductForm("Dean");
        });

        HBox buttons = new HBox(20, hodBtn, deanBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(30, title, buttons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        root.setStyle(
                "-fx-background-color:#F5DEB3;" +
                "-fx-background-radius:10;" +
                "-fx-border-color:#8B5E34;" +
                "-fx-border-width:2;"
        );

        stage.setScene(new Scene(root,350,200));
        stage.setTitle("Request Inventory");
        stage.show();
    }
    private void showProductForm(String recipient) {

    Stage stage = new Stage();

    Label title = new Label("Inventory Request");
    title.setStyle("-fx-font-size:22; -fx-font-weight:bold;");

    TextField product = new TextField();
    product.setPromptText("Product Name");

    TextField qty = new TextField();
    qty.setPromptText("Product Quantity");

    Button sendBtn = new Button("Send Request");
    sendBtn.setStyle(
            "-fx-background-color:#8B5E34;" +
            "-fx-text-fill:white;" +
            "-fx-font-size:16;"
    );

    sendBtn.setOnAction(e -> {
        stage.close();
        confirmEmailSend(recipient, product.getText(), qty.getText());
    });

    VBox root = new VBox(15,
            title,
            new Label("Product Name"), product,
            new Label("Quantity"), qty,
            sendBtn
    );

    root.setPadding(new Insets(30));
    root.setAlignment(Pos.CENTER);
    root.setStyle("-fx-background-color:#F5DEB3;");

    stage.setScene(new Scene(root,350,300));
    stage.show();
}
    private void confirmEmailSend(String recipient,
                              String productName,
                              String quantity) {

    Stage stage = new Stage();

    Label title = new Label("Confirm Request");
    title.setStyle("-fx-font-size:20; -fx-font-weight:bold;");

    Label msg = new Label("Do you really want to send the email?");
    msg.setStyle("-fx-font-size:14;");

    Button confirmBtn = new Button("Confirm");
    Button cancelBtn = new Button("Cancel");

    confirmBtn.setStyle(
            "-fx-background-color:#8B5E34;" +
            "-fx-text-fill:white;" +
            "-fx-font-size:14;"
    );

    cancelBtn.setStyle(
            "-fx-background-color:#D7B98E;" +
            "-fx-text-fill:black;" +
            "-fx-font-size:14;"
    );

    confirmBtn.setOnAction(e -> {

        stage.close();
        
        sendEmail(recipient, productName, quantity);   // ⭐ EMAIL SEND

        // Notification add
        NotificationService.addNotification(
            "Inventory request sent to " + recipient +
            "\nProduct : " + productName +
            "\nQuantity : " + quantity
        );
    });

    cancelBtn.setOnAction(e -> stage.close());

    HBox buttons = new HBox(15, confirmBtn, cancelBtn);
    buttons.setAlignment(Pos.CENTER);

    VBox root = new VBox(20, title, msg, buttons);
    root.setAlignment(Pos.CENTER);
    root.setPadding(new Insets(30));

    root.setStyle(
            "-fx-background-color:#F5DEB3;" +
            "-fx-border-color:#8B5E34;" +
            "-fx-border-width:2;" +
            "-fx-background-radius:10;"
    );

    stage.setScene(new Scene(root,350,180));
    stage.show();
}
    private void sendEmail(String recipient, String productName, String quantity) {

    String toEmail = "";

    try(Connection con = db.DBConnection.getConnection();
        Statement st = con.createStatement()) {

        String query;

        // Fetch email from database
        if(recipient.equals("HOD")) {
            query = "SELECT email FROM user WHERE role_id = 2";
        } else {
            query = "SELECT email FROM user WHERE role_id = 3";
        }

        ResultSet rs = st.executeQuery(query);

        if(rs.next()){
            toEmail = rs.getString("email");
        }

        System.out.println("Recipient Role : " + recipient);
        System.out.println("Email fetched from DB : " + toEmail);

    } catch(Exception e){
        e.printStackTrace();
    }

    // Email subject
    String subject = "Inventory Request";

    // Email body
    String body = "Respected Sir,\n\n"
        + "This is to inform you that a requirement for inventory items has been identified in the "
        + "AIM & ACT Department (CS), Banasthali Vidyapith.\n\n"
        + "Kindly approve the procurement as per the details mentioned below:\n\n"
        + "Product Name : " + productName + "\n"
        + "Required Quantity : " + quantity + " units\n\n"
        + "These items are required for departmental use. You are kindly requested to grant approval "
        + "to proceed with the necessary procurement at the earliest.\n\n"
        + "Thank you for your support and consideration.\n\n"
        + "Regards,\n"
        + "Inventory Manager\n"
        + "AIM & ACT Department (CS)\n"
        + "Banasthali Vidyapith";

    try {

        Properties props = new Properties();

        props.put("mail.smtp.host","smtp.gmail.com");
        props.put("mail.smtp.port","587");
        props.put("mail.smtp.auth","true");
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.starttls.required","true");
        props.put("mail.smtp.ssl.trust","smtp.gmail.com");

        Session session = Session.getInstance(props,
            new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {

                    return new PasswordAuthentication(
                        "g7447931@gmail.com",   // sender email
                        "wducnqifrnmhxivd"      // app password
                    );
                }
        });
        session.setDebug(true);   // ⭐ SMTP debug

        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress("g7447931@gmail.com"));

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail)
        );

        message.setSubject(subject);
        message.setText(body);
            
        Transport.send(message);

        System.out.println("Email Sent Successfully to : " + toEmail);
        showSuccessMessage();
        
        if(toEmail.isEmpty()){
            System.out.println("No email found for role : " + recipient);
            return;
            
        }

    } catch(Exception e){
        e.printStackTrace();
    }
}
    private void showSuccessMessage() {

    Stage stage = new Stage();

    Label title = new Label("Email Sent");
    title.setStyle("-fx-font-size:20; -fx-font-weight:bold;");

    Label msg = new Label("Inventory request email has been sent successfully.");
    msg.setWrapText(true);

    Button ok = new Button("OK");

    ok.setStyle(
            "-fx-background-color:#8B5E34;" +
            "-fx-text-fill:white;" +
            "-fx-font-size:14;"
    );

    ok.setOnAction(e -> stage.close());

    VBox root = new VBox(20, title, msg, ok);
    root.setAlignment(Pos.CENTER);
    root.setPadding(new Insets(30));

    root.setStyle(
            "-fx-background-color:#F5DEB3;" +
            "-fx-border-color:#8B5E34;" +
            "-fx-border-width:2;" +
            "-fx-background-radius:10;"
    );

    stage.setScene(new Scene(root,350,180));
    stage.show();
}
    public Scene getScene() {
        return scene;
    }
}