package ui;

import com.mycompany.inventory.Inventory;
import db.DBConnection;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;

import java.sql.*;

public class ManageInventoryUI {

    private Scene scene;

    private FilteredList<PRow> filteredProducts;
    private FilteredList<SRow> filteredSuppliers;
    private FilteredList<ORow> filteredOrders;
    private FilteredList<BRow> filteredBills;

    // ===================== STYLE CONSTANTS =====================
    private static final String BG_COLOR   = "#E2C49F";
    private static final String CARD_STYLE = """
            -fx-background-color: rgba(255,255,255,0.92);
            -fx-background-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0.4, 0, 10);
            """;
    private static final String FIELD_STYLE = """
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: #C49A6C;
            -fx-border-width: 1.5;
            -fx-background-color: #FFF6E9;
            -fx-font-size: 15px;
            """;
    private static final String BTN_BROWN =
            "-fx-background-color:#8B5E34; -fx-text-fill:white; " +
            "-fx-font-size:14px; -fx-font-weight:bold; " +
            "-fx-background-radius:8; -fx-padding:10 28 10 28;";
    private static final String BTN_RED =
            "-fx-background-color:#c0392b; -fx-text-fill:white; " +
            "-fx-font-size:14px; -fx-font-weight:bold; " +
            "-fx-background-radius:8; -fx-padding:10 28 10 28;";
    private static final String TAB_STYLE =
            "-fx-font-size:15px; -fx-font-weight:bold;";

    // ===================== SEARCH BAR STYLE =====================
    private static final String SEARCH_STYLE = """
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: #C49A6C;
            -fx-border-width: 1.5;
            -fx-background-color: #FFF6E9;
            -fx-font-size: 14px;
            -fx-prompt-text-fill: #9E7B55;
            """;

    // ===================== CONSTRUCTOR =====================
    public ManageInventoryUI(Inventory app) {

        Label title = new Label("Manage Inventory Data");
        title.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 26));
        title.setStyle("-fx-text-fill:#3E2723;");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-tab-min-height:40px;");

        Tab productTab  = new Tab("  Product  ");
        Tab supplierTab = new Tab("  Supplier  ");
        Tab orderTab    = new Tab("  Orders  ");
        Tab billTab     = new Tab("  Bills  ");

        productTab.setStyle(TAB_STYLE);
        supplierTab.setStyle(TAB_STYLE);
        orderTab.setStyle(TAB_STYLE);
        billTab.setStyle(TAB_STYLE);

        // ── Build each tab and keep table references for live reload ──
        ScrollPane productSP  = buildProductContent();
        ScrollPane supplierSP = buildSupplierContent();

        // Order and Bill tables are kept as fields so delete can reload them
        TableView<ORow> orderTable = new TableView<>();
        TableView<BRow> billTable  = new TableView<>();

        ScrollPane orderSP = buildOrderContent(orderTable, billTable);
        ScrollPane billSP  = buildBillContent(billTable, orderTable);

        productTab.setContent(productSP);
        supplierTab.setContent(supplierSP);
        orderTab.setContent(orderSP);
        billTab.setContent(billSP);

        tabPane.getTabs().addAll(productTab, supplierTab, orderTab, billTab);

        Button addBtn  = new Button("+ Add New");
        Button backBtn = new Button("Back");

        addBtn.setStyle(BTN_BROWN);
        backBtn.setStyle(BTN_BROWN);

        // Hide Add on Order & Bill tab
        tabPane.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, now) -> {
                if (now != null) {
                    addBtn.setVisible(!(now.equals(orderTab) || now.equals(billTab)));
                }
            });

        addBtn.setOnAction(e -> {
            Tab current = tabPane.getSelectionModel().getSelectedItem();
            if (current.equals(productTab)) {
                showAddProductForm((VBox) productSP.getContent());
            } else if (current.equals(supplierTab)) {
                showAddSupplierForm((VBox) supplierSP.getContent());
            }
        });

        backBtn.setOnAction(e -> app.showDashboard());

        HBox bottomBar = new HBox(20, addBtn, backBtn);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(15, 30, 15, 30));
        bottomBar.setStyle(
                "-fx-background-color:#d9b896; -fx-background-radius:0 0 15 15;");

        VBox card = new VBox(20, title, tabPane);
        card.setPadding(new Insets(30));
        card.setStyle(CARD_STYLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG_COLOR + ";");
        root.setCenter(card);
        root.setBottom(bottomBar);
        BorderPane.setMargin(card, new Insets(30, 30, 10, 30));

        scene = new Scene(root, 1100, 720);
    }

    // =====================================================================
    //  SEARCH BAR HELPER — reusable styled search field
    // =====================================================================

    private TextField buildSearchField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(SEARCH_STYLE);
        tf.setPrefHeight(38);
        tf.setMaxWidth(340);
        return tf;
    }

    // =====================================================================
    //  PRODUCT TAB
    //  Shows: Name, Description, Min Qty, Stock, Type   (pid hidden)
    //  Editable: Name, Description, Min Qty
    //  NOT editable: Stock, Type
    // =====================================================================

    private static class PRow {
        StringProperty  pid    = new SimpleStringProperty();
        StringProperty  name   = new SimpleStringProperty();
        IntegerProperty qty    = new SimpleIntegerProperty();
        StringProperty  desc   = new SimpleStringProperty();
        IntegerProperty minQty = new SimpleIntegerProperty();
        StringProperty  ptype  = new SimpleStringProperty();

        PRow(String pid, String name, int qty, String desc, int minQty, String ptype) {
            this.pid.set(pid);   this.name.set(name);
            this.qty.set(qty);   this.desc.set(desc);
            this.minQty.set(minQty); this.ptype.set(ptype);
        }
    }

    private ScrollPane buildProductContent() {
        TableView<PRow> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size:14px;");

        TableColumn<PRow,String>  nameCol  = col("Name",        220);
        TableColumn<PRow,String>  descCol  = col("Description", 300);
        TableColumn<PRow,Integer> minCol   = colInt("Min Qty",  110);
        TableColumn<PRow,Integer> qtyCol   = colInt("Stock",    110);
        TableColumn<PRow,String>  ptypeCol = col("Type",        130);

        nameCol.setCellValueFactory(d  -> d.getValue().name);
        descCol.setCellValueFactory(d  -> d.getValue().desc);
        minCol.setCellValueFactory(d   -> d.getValue().minQty.asObject());
        qtyCol.setCellValueFactory(d   -> d.getValue().qty.asObject());
        ptypeCol.setCellValueFactory(d -> d.getValue().ptype);

        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> { e.getRowValue().name.set(e.getNewValue()); updateProduct(e.getRowValue()); });

        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(e -> { e.getRowValue().desc.set(e.getNewValue()); updateProduct(e.getRowValue()); });

        minCol.setCellFactory(TextFieldTableCell.forTableColumn(
                new javafx.util.converter.IntegerStringConverter()));
        minCol.setOnEditCommit(e -> { e.getRowValue().minQty.set(e.getNewValue()); updateProduct(e.getRowValue()); });

        table.getColumns().addAll(nameCol, descCol, minCol, qtyCol, ptypeCol);

        ObservableList<PRow> masterData = FXCollections.observableArrayList();
        filteredProducts = new FilteredList<>(masterData, p -> true);
        table.setItems(filteredProducts);
        loadProducts(masterData);

        // ── Search bar wired to filteredProducts ──
        TextField searchField = buildSearchField("🔍  Search by name, description or type…");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String lower = newVal == null ? "" : newVal.toLowerCase().trim();
            filteredProducts.setPredicate(row -> {
                if (lower.isEmpty()) return true;
                return row.name.get().toLowerCase().contains(lower)
                    || row.desc.get().toLowerCase().contains(lower)
                    || row.ptype.get().toLowerCase().contains(lower);
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button resetBtn = new Button("✖");
        resetBtn.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-weight:bold;");
        resetBtn.setOnAction(e -> searchField.clear());

        HBox searchBar = new HBox(10, spacer, searchField, resetBtn);
        searchBar.setAlignment(Pos.CENTER_RIGHT);
        searchBar.setPadding(new Insets(8, 0, 8, 0));

        VBox box = new VBox(searchBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setUserData(table);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true); sp.setFitToHeight(true);
        sp.setStyle("-fx-background-color:transparent;");
        return sp;
    }

    private void loadProducts(ObservableList<PRow> list) {
        list.clear();
        String sql = "SELECT pid, product_name, qty_in_stock, description, " +
                     "min_qty_required, ptype_id FROM product";
        try (Connection con = DBConnection.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new PRow(
                        rs.getString("pid"),
                        rs.getString("product_name"),
                        rs.getInt("qty_in_stock"),
                        rs.getString("description"),
                        rs.getInt("min_qty_required"),
                        rs.getString("ptype_id")
                ));
            }
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void updateProduct(PRow row) {
        String sql = "UPDATE product SET product_name=?, qty_in_stock=?, " +
                     "description=?, min_qty_required=? WHERE pid=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, row.name.get()); ps.setInt(2, row.qty.get());
            ps.setString(3, row.desc.get()); ps.setInt(4, row.minQty.get());
            ps.setString(5, row.pid.get());
            ps.executeUpdate();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void showAddProductForm(VBox contentBox) {
        @SuppressWarnings("unchecked")
        TableView<PRow> table = (TableView<PRow>) contentBox.getUserData();

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add New Product");
        dlg.setHeaderText("Enter product details");
        GridPane g = formGrid();

        TextField pidF  = field();
        pidF.setText(generateNextProductId());
        pidF.setEditable(false);
        TextField nameF = field();
        TextField qtyF  = field(); TextField descF = field();
        TextField minF  = field();
        ComboBox<String> ptypeBox = new ComboBox<>();
        ptypeBox.getItems().addAll("STN", "FUR", "CS", "Misc");
        ptypeBox.setStyle(FIELD_STYLE); ptypeBox.setPrefSize(260, 42);

        g.addRow(0, lbl("PID"),         pidF);
        g.addRow(1, lbl("Name"),        nameF);
        g.addRow(2, lbl("Description"), descF);
        g.addRow(3, lbl("Stock"),       qtyF);
        g.addRow(4, lbl("Min Qty"),     minF);
        g.addRow(5, lbl("Type"),        ptypeBox);

        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().setStyle("-fx-background-color:#F5DEB3;");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String sql = "INSERT INTO product (pid, product_name, description, " +
                         "qty_in_stock, min_qty_required, ptype_id) VALUES (?,?,?,?,?,?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, pidF.getText().trim());
                ps.setString(2, nameF.getText().trim());
                ps.setString(3, descF.getText().trim());
                ps.setInt(4,    Integer.parseInt(qtyF.getText().trim()));
                ps.setInt(5,    Integer.parseInt(minF.getText().trim()));
                ps.setString(6, ptypeBox.getValue());
                ps.executeUpdate();
                ObservableList<PRow> list = FXCollections.observableArrayList();
                loadProducts(list);
                table.setItems(list);
            } catch (Exception e) { showError(e.getMessage()); }
        });
    }

    // =====================================================================
    //  SUPPLIER TAB
    //  Shows: Name, Email, Contact, Address, Type   (sid hidden)
    //  Editable: Name, Email, Contact, Address   NOT editable: Type
    // =====================================================================

    private static class SRow {
        IntegerProperty sid     = new SimpleIntegerProperty();
        StringProperty  name    = new SimpleStringProperty();
        StringProperty  email   = new SimpleStringProperty();
        StringProperty  contact = new SimpleStringProperty();
        StringProperty  address = new SimpleStringProperty();
        StringProperty  ptype   = new SimpleStringProperty();

        SRow(int sid, String name, String email, String contact,
             String address, String ptype) {
            this.sid.set(sid);       this.name.set(name);
            this.email.set(email);   this.contact.set(contact);
            this.address.set(address); this.ptype.set(ptype);
        }
    }

    private ScrollPane buildSupplierContent() {
        TableView<SRow> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size:14px;");

        TableColumn<SRow,String> nameCol    = col("Name",    200);
        TableColumn<SRow,String> emailCol   = col("Email",   220);
        TableColumn<SRow,String> contactCol = col("Contact", 150);
        TableColumn<SRow,String> addrCol    = col("Address", 250);
        TableColumn<SRow,String> ptypeCol   = col("Type",    110);

        nameCol.setCellValueFactory(d    -> d.getValue().name);
        emailCol.setCellValueFactory(d   -> d.getValue().email);
        contactCol.setCellValueFactory(d -> d.getValue().contact);
        addrCol.setCellValueFactory(d    -> d.getValue().address);
        ptypeCol.setCellValueFactory(d   -> d.getValue().ptype);

        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> { e.getRowValue().name.set(e.getNewValue()); updateSupplier(e.getRowValue()); });
        emailCol.setCellFactory(TextFieldTableCell.forTableColumn());
        emailCol.setOnEditCommit(e -> { e.getRowValue().email.set(e.getNewValue()); updateSupplier(e.getRowValue()); });
        contactCol.setCellFactory(TextFieldTableCell.forTableColumn());
        contactCol.setOnEditCommit(e -> { e.getRowValue().contact.set(e.getNewValue()); updateSupplier(e.getRowValue()); });
        addrCol.setCellFactory(TextFieldTableCell.forTableColumn());
        addrCol.setOnEditCommit(e -> { e.getRowValue().address.set(e.getNewValue()); updateSupplier(e.getRowValue()); });

        table.getColumns().addAll(nameCol, emailCol, contactCol, addrCol, ptypeCol);

        ObservableList<SRow> masterData = FXCollections.observableArrayList();
        filteredSuppliers = new FilteredList<>(masterData, p -> true);
        table.setItems(filteredSuppliers);
        loadSuppliers(masterData);

        // ── Search bar wired to filteredSuppliers ──
        TextField searchField = buildSearchField("🔍  Search by name, email, contact or address…");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String lower = newVal == null ? "" : newVal.toLowerCase().trim();
            filteredSuppliers.setPredicate(row -> {
                if (lower.isEmpty()) return true;
                return row.name.get().toLowerCase().contains(lower)
                    || row.email.get().toLowerCase().contains(lower)
                    || row.contact.get().toLowerCase().contains(lower)
                    || row.address.get().toLowerCase().contains(lower)
                    || row.ptype.get().toLowerCase().contains(lower);
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button resetBtn = new Button("✖");
        resetBtn.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-weight:bold;");
        resetBtn.setOnAction(e -> searchField.clear());

        HBox searchBar = new HBox(10, spacer, searchField, resetBtn);
        searchBar.setAlignment(Pos.CENTER_RIGHT);
        searchBar.setPadding(new Insets(8, 0, 8, 0));

        VBox box = new VBox(searchBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setUserData(table);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true); sp.setFitToHeight(true);
        sp.setStyle("-fx-background-color:transparent;");
        return sp;
    }

    private void loadSuppliers(ObservableList<SRow> list) {
        list.clear();
        String sql = "SELECT sid, name, email, contact_no, address, ptype_id FROM supplier";
        try (Connection con = DBConnection.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new SRow(
                        rs.getInt("sid"),
                        rs.getString("name"),
                        rs.getString("email") == null ? "" : rs.getString("email"),
                        rs.getString("contact_no"),
                        rs.getString("address"),
                        rs.getString("ptype_id")
                ));
            }
        } catch (Exception e) { showError(e.getMessage()); }
    }

    // ── loadSuppliers overload that accepts a TableView (used by showAddSupplierForm) ──
    private void loadSuppliers(TableView<SRow> table) {
        ObservableList<SRow> masterData = FXCollections.observableArrayList();
        filteredSuppliers = new FilteredList<>(masterData, p -> true);
        loadSuppliers(masterData);
        table.setItems(filteredSuppliers);
    }

    private void updateSupplier(SRow row) {
        String sql = "UPDATE supplier SET name=?, email=?, contact_no=?, address=? WHERE sid=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, row.name.get()); ps.setString(2, row.email.get());
            ps.setString(3, row.contact.get()); ps.setString(4, row.address.get());
            ps.setInt(5, row.sid.get());
            ps.executeUpdate();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void showAddSupplierForm(VBox contentBox) {
        @SuppressWarnings("unchecked")
        TableView<SRow> table = (TableView<SRow>) contentBox.getUserData();

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add New Supplier");
        dlg.setHeaderText("Enter supplier details");
        GridPane g = formGrid();

        TextField nameF = field(); TextField emailF = field();
        TextField contactF = field(); TextField addrF = field();
        ComboBox<String> ptypeBox = new ComboBox<>();
        ptypeBox.getItems().addAll("STN", "FUR", "CS", "Misc");
        ptypeBox.setStyle(FIELD_STYLE); ptypeBox.setPrefSize(260, 42);

        g.addRow(0, lbl("Name"),    nameF);
        g.addRow(1, lbl("Email"),   emailF);
        g.addRow(2, lbl("Contact"), contactF);
        g.addRow(3, lbl("Address"), addrF);
        g.addRow(4, lbl("Type"),    ptypeBox);

        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().setStyle("-fx-background-color:#F5DEB3;");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String sql = "INSERT INTO supplier (name, email, contact_no, address, ptype_id) VALUES (?,?,?,?,?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nameF.getText().trim()); ps.setString(2, emailF.getText().trim());
                ps.setString(3, contactF.getText().trim()); ps.setString(4, addrF.getText().trim());
                ps.setString(5, ptypeBox.getValue());
                ps.executeUpdate();
                loadSuppliers(table);   // uses the overload above — re-wraps in FilteredList
            } catch (Exception e) { showError(e.getMessage()); }
        });
    }

    // =====================================================================
    //  ORDER ROW — entryId, pid, sid kept for DB ops but NOT shown
    // =====================================================================

    private static class ORow {
        IntegerProperty entryId      = new SimpleIntegerProperty();
        StringProperty  pid          = new SimpleStringProperty();
        IntegerProperty sid          = new SimpleIntegerProperty();
        StringProperty  orderNo      = new SimpleStringProperty();
        StringProperty  productName  = new SimpleStringProperty();
        StringProperty  supplierName = new SimpleStringProperty();
        StringProperty  orderDate    = new SimpleStringProperty();
        IntegerProperty qtyOrdered   = new SimpleIntegerProperty();
        StringProperty  orderStatus  = new SimpleStringProperty();

        ORow(int entryId, String pid, int sid,
             String orderNo, String productName, String supplierName,
             String orderDate, int qtyOrdered, String orderStatus) {
            this.entryId.set(entryId);
            this.pid.set(pid == null ? "" : pid);
            this.sid.set(sid);
            this.orderNo.set(orderNo == null ? "" : orderNo);
            this.productName.set(productName == null ? "" : productName);
            this.supplierName.set(supplierName == null ? "" : supplierName);
            this.orderDate.set(orderDate == null ? "" : orderDate);
            this.qtyOrdered.set(qtyOrdered);
            this.orderStatus.set(orderStatus == null ? "" : orderStatus);
        }
    }

    // =====================================================================
    //  BILL ROW — entryId, pid, billId kept for DB ops but NOT shown
    // =====================================================================

    private static class BRow {
        IntegerProperty entryId      = new SimpleIntegerProperty();
        StringProperty  pid          = new SimpleStringProperty();
        IntegerProperty billId       = new SimpleIntegerProperty();
        StringProperty  billNo       = new SimpleStringProperty();
        StringProperty  orderNo      = new SimpleStringProperty();
        StringProperty  productName  = new SimpleStringProperty();
        StringProperty  supplierName = new SimpleStringProperty();
        StringProperty  receivedBy   = new SimpleStringProperty();
        DoubleProperty  billAmount   = new SimpleDoubleProperty();
        IntegerProperty qtyReceived  = new SimpleIntegerProperty();
        StringProperty  receivedDate = new SimpleStringProperty();
        StringProperty  billStatus   = new SimpleStringProperty();

        BRow(int entryId, String pid, int billId, String billNo,
             String orderNo, String productName, String supplierName,
             String receivedBy, double billAmount, int qtyReceived,
             String receivedDate, String billStatus) {
            this.entryId.set(entryId);
            this.pid.set(pid == null ? "" : pid);
            this.billId.set(billId);
            this.billNo.set(billNo == null ? "" : billNo);
            this.orderNo.set(orderNo == null ? "" : orderNo);
            this.productName.set(productName == null ? "" : productName);
            this.supplierName.set(supplierName == null ? "" : supplierName);
            this.receivedBy.set(receivedBy == null ? "" : receivedBy);
            this.billAmount.set(billAmount);
            this.qtyReceived.set(qtyReceived);
            this.receivedDate.set(receivedDate == null ? "" : receivedDate);
            this.billStatus.set(billStatus == null ? "" : billStatus);
        }
    }

    // =====================================================================
    //  ORDER TAB — read-only, delete only
    //  ✅ Receives billTable reference so it can reload bills after order delete
    // =====================================================================

    private ScrollPane buildOrderContent(TableView<ORow> table,
                                         TableView<BRow> billTable) {
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size:14px;");

        TableColumn<ORow,String>  orderNoCol  = col("Order No",      130);
        TableColumn<ORow,String>  prodNameCol = col("Product Name",  180);
        TableColumn<ORow,String>  suppNameCol = col("Supplier Name", 180);
        TableColumn<ORow,String>  oDateCol    = col("Order Date",    120);
        TableColumn<ORow,Integer> qtyOrdCol   = colInt("Qty Ordered",110);
        TableColumn<ORow,String>  oStatusCol  = col("Order Status",  120);
        TableColumn<ORow,Void>    delCol      = new TableColumn<>("Delete");

        orderNoCol.setCellValueFactory(d  -> d.getValue().orderNo);
        prodNameCol.setCellValueFactory(d -> d.getValue().productName);
        suppNameCol.setCellValueFactory(d -> d.getValue().supplierName);
        oDateCol.setCellValueFactory(d    -> d.getValue().orderDate);
        qtyOrdCol.setCellValueFactory(d   -> d.getValue().qtyOrdered.asObject());
        oStatusCol.setCellValueFactory(d  -> d.getValue().orderStatus);

        delCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.setStyle(BTN_RED);
                btn.setOnAction(e -> {
                    ORow row = getTableView().getItems().get(getIndex());
                    if (confirmDelete("order '" + row.orderNo.get() + "'")) {
                        try (Connection con = DBConnection.getConnection()) {
                            // Block delete if any active bill exists for this entry
                            PreparedStatement check = con.prepareStatement(
                                "SELECT COUNT(*) FROM bill_invoice " +
                                "WHERE entry_id = ? AND record_status = 'ACTIVE'");
                            check.setInt(1, row.entryId.get());
                            ResultSet rs = check.executeQuery();
                            rs.next();
                            if (rs.getInt(1) > 0) {
                                showError("Cannot delete order. " +
                                          "Delete all its bills first.");
                                return;
                            }
                            // Safe to delete — soft-delete in DB
                            softDeleteOrder(row.entryId.get());
                            // ✅ Live reload: remove from UI immediately
                            getTableView().getItems().remove(row);
                        } catch (Exception ex) { showError(ex.getMessage()); }
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(
            orderNoCol, prodNameCol, suppNameCol,
            oDateCol, qtyOrdCol, oStatusCol, delCol);

        ObservableList<ORow> masterData = FXCollections.observableArrayList();
        filteredOrders = new FilteredList<>(masterData, p -> true);
        table.setItems(filteredOrders);
        loadOrders(masterData);

        // ── Search bar wired to filteredOrders ──
        TextField searchField = buildSearchField("🔍  Search by order no, product, supplier or status…");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String lower = newVal == null ? "" : newVal.toLowerCase().trim();
            filteredOrders.setPredicate(row -> {
                if (lower.isEmpty()) return true;
                return row.orderNo.get().toLowerCase().contains(lower)
                    || row.productName.get().toLowerCase().contains(lower)
                    || row.supplierName.get().toLowerCase().contains(lower)
                    || row.orderStatus.get().toLowerCase().contains(lower)
                    || row.orderDate.get().toLowerCase().contains(lower);
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button resetBtn = new Button("✖");
        resetBtn.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-weight:bold;");
        resetBtn.setOnAction(e -> searchField.clear());

        HBox searchBar = new HBox(10, spacer, searchField, resetBtn);
        searchBar.setAlignment(Pos.CENTER_RIGHT);
        searchBar.setPadding(new Insets(8, 0, 8, 0));

        VBox box = new VBox(searchBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setUserData(table);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true); sp.setFitToHeight(true);
        sp.setStyle("-fx-background-color:transparent;");
        return sp;
    }

    // =====================================================================
    //  BILL TAB — read-only, delete only
    //  ✅ Receives orderTable reference so it can reload orders after bill delete
    // =====================================================================

    private ScrollPane buildBillContent(TableView<BRow> table,
                                        TableView<ORow> orderTable) {
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size:14px;");

        TableColumn<BRow,String>  billNoCol   = col("Bill No",       110);
        TableColumn<BRow,String>  orderNoCol  = col("Order No",      110);
        TableColumn<BRow,String>  prodNameCol = col("Product Name",  150);
        TableColumn<BRow,String>  suppNameCol = col("Supplier Name", 150);
        TableColumn<BRow,String>  recByCol    = col("Received By",   130);
        TableColumn<BRow,Double>  amtCol      = new TableColumn<>("Amount");
        TableColumn<BRow,Integer> qtyRecCol   = colInt("Qty Received",110);
        TableColumn<BRow,String>  recDateCol  = col("Received Date", 120);
        TableColumn<BRow,String>  bStatusCol  = col("Bill Status",   110);
        TableColumn<BRow,Void>    delCol      = new TableColumn<>("Delete");

        amtCol.setPrefWidth(100);

        billNoCol.setCellValueFactory(d   -> d.getValue().billNo);
        orderNoCol.setCellValueFactory(d  -> d.getValue().orderNo);
        prodNameCol.setCellValueFactory(d -> d.getValue().productName);
        suppNameCol.setCellValueFactory(d -> d.getValue().supplierName);
        recByCol.setCellValueFactory(d    -> d.getValue().receivedBy);
        amtCol.setCellValueFactory(d      -> d.getValue().billAmount.asObject());
        qtyRecCol.setCellValueFactory(d   -> d.getValue().qtyReceived.asObject());
        recDateCol.setCellValueFactory(d  -> d.getValue().receivedDate);
        bStatusCol.setCellValueFactory(d  -> d.getValue().billStatus);

        delCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.setStyle(BTN_RED);
                btn.setOnAction(e -> {
                    BRow row = getTableView().getItems().get(getIndex());
                    if (confirmDelete("bill '" + row.billNo.get() + "'")) {

                        // ✅ CORRECT ORDER:
                        // 1. Soft-delete first (marks INACTIVE, so recalc excludes it)
                        // 2. Reverse stock
                        // 3. Recalculate statuses (reads only remaining ACTIVE bills)
                        // 4. Reload both tables live — no navigation needed
                        softDeleteBill(row.billId.get());
                        reverseStock(row);
                        recalculateStatuses(row.entryId.get());

                        // ✅ Live reload bill table from DB
                        loadBills(table);

                        // ✅ Live reload order table from DB
                        // (order_status may have changed from PAID → IN_PROCESS)
                        loadOrders(orderTable);
                    }
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(
            billNoCol, orderNoCol, prodNameCol, suppNameCol,
            recByCol, amtCol, qtyRecCol, recDateCol, bStatusCol, delCol);

        ObservableList<BRow> masterData = FXCollections.observableArrayList();
        filteredBills = new FilteredList<>(masterData, p -> true);
        table.setItems(filteredBills);
        loadBills(masterData);

        // ── Search bar wired to filteredBills ──
        TextField searchField = buildSearchField("🔍  Search by bill no, order no, product, supplier or status…");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String lower = newVal == null ? "" : newVal.toLowerCase().trim();
            filteredBills.setPredicate(row -> {
                if (lower.isEmpty()) return true;
                return row.billNo.get().toLowerCase().contains(lower)
                    || row.orderNo.get().toLowerCase().contains(lower)
                    || row.productName.get().toLowerCase().contains(lower)
                    || row.supplierName.get().toLowerCase().contains(lower)
                    || row.receivedBy.get().toLowerCase().contains(lower)
                    || row.billStatus.get().toLowerCase().contains(lower)
                    || row.receivedDate.get().toLowerCase().contains(lower);
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button resetBtn = new Button("✖");
        resetBtn.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-weight:bold;");
        resetBtn.setOnAction(e -> searchField.clear());

        HBox searchBar = new HBox(10, spacer, searchField, resetBtn);
        searchBar.setAlignment(Pos.CENTER_RIGHT);
        searchBar.setPadding(new Insets(8, 0, 8, 0));

        VBox box = new VBox(searchBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setUserData(table);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true); sp.setFitToHeight(true);
        sp.setStyle("-fx-background-color:transparent;");
        return sp;
    }

    // =====================================================================
    //  LOAD METHODS
    // =====================================================================

    private void loadOrders(ObservableList<ORow> list) {
        list.clear();
        String sql =
            "SELECT o.entry_id, o.pid, o.sid, o.order_no, " +
            "       p.product_name, s.name AS supplier_name, " +
            "       o.order_date, o.qty_ordered, o.order_status " +
            "FROM order_table o " +
            "JOIN product  p ON p.pid = o.pid " +
            "JOIN supplier s ON s.sid = o.sid " +
            "WHERE o.record_status = 'ACTIVE' " +
            "ORDER BY o.entry_id";
        try (Connection con = DBConnection.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ORow(
                        rs.getInt("entry_id"),
                        rs.getString("pid"),
                        rs.getInt("sid"),
                        rs.getString("order_no"),
                        rs.getString("product_name"),
                        rs.getString("supplier_name"),
                        rs.getString("order_date"),
                        rs.getInt("qty_ordered"),
                        rs.getString("order_status")
                ));
            }
        } catch (Exception e) { showError(e.getMessage()); }
    }

    /**
     * Live-reload overload used by the bill-delete handler.
     * Replaces the TableView's items with a fresh FilteredList so
     * the existing search predicate is preserved via filteredOrders.
     */
    private void loadOrders(TableView<ORow> table) {
        ObservableList<ORow> masterData = FXCollections.observableArrayList();
        filteredOrders = new FilteredList<>(masterData, p -> true);
        loadOrders(masterData);
        table.setItems(filteredOrders);
    }

    private void loadBills(ObservableList<BRow> list) {
        list.clear();
        String sql =
            "SELECT b.entry_id, b.pid, b.bill_id, b.bill_no, " +
            "       o.order_no, p.product_name, s.name AS supplier_name, " +
            "       b.bill_received_by, b.bill_amount, b.qty_received, " +
            "       b.received_date, b.bill_status " +
            "FROM bill_invoice b " +
            "JOIN order_table o ON o.entry_id = b.entry_id " +
            "JOIN product     p ON p.pid      = b.pid " +
            "JOIN supplier    s ON s.sid      = b.sid " +
            "WHERE b.record_status = 'ACTIVE' " +
            "ORDER BY b.bill_id";
        try (Connection con = DBConnection.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new BRow(
                        rs.getInt("entry_id"),
                        rs.getString("pid"),
                        rs.getInt("bill_id"),
                        rs.getString("bill_no"),
                        rs.getString("order_no"),
                        rs.getString("product_name"),
                        rs.getString("supplier_name"),
                        rs.getString("bill_received_by"),
                        rs.getDouble("bill_amount"),
                        rs.getInt("qty_received"),
                        rs.getString("received_date"),
                        rs.getString("bill_status")
                ));
            }
        } catch (Exception e) { showError(e.getMessage()); }
    }

    /**
     * Live-reload overload used by the bill-delete handler.
     * Replaces the TableView's items with a fresh FilteredList so
     * the existing search predicate is preserved via filteredBills.
     */
    private void loadBills(TableView<BRow> table) {
        ObservableList<BRow> masterData = FXCollections.observableArrayList();
        filteredBills = new FilteredList<>(masterData, p -> true);
        loadBills(masterData);
        table.setItems(filteredBills);
    }

    // =====================================================================
    //  UPDATE METHOD (order qty — blocked if bill exists)
    // =====================================================================

    private void updateOrder(ORow row) {
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement check = con.prepareStatement(
                "SELECT COUNT(*) FROM bill_invoice " +
                "WHERE entry_id=? AND record_status='ACTIVE'");
            check.setInt(1, row.entryId.get());
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                showError("Cannot edit order quantity after a bill has been created.");
                return;
            }
            PreparedStatement ps = con.prepareStatement(
                "UPDATE order_table SET qty_ordered=? WHERE entry_id=?");
            ps.setInt(1, row.qtyOrdered.get());
            ps.setInt(2, row.entryId.get());
            ps.executeUpdate();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    // =====================================================================
    //  DELETE HELPERS
    // =====================================================================

    private void softDeleteBill(int billId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE bill_invoice SET record_status='INACTIVE' WHERE bill_id=?")) {
            ps.setInt(1, billId);
            ps.executeUpdate();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void reverseStock(BRow row) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE product SET qty_in_stock = qty_in_stock - ? WHERE pid = ?")) {
            ps.setInt(1, row.qtyReceived.get());
            ps.setString(2, row.pid.get());
            ps.executeUpdate();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void softDeleteOrder(int entryId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE order_table SET record_status='INACTIVE' WHERE entry_id=?")) {
            ps.setInt(1, entryId);
            ps.executeUpdate();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    // =====================================================================
    //  RECALCULATE STATUSES
    //  Called AFTER softDeleteBill() — deleted bill is already INACTIVE.
    //  Pure Java arithmetic, no correlated subquery conflicts.
    // =====================================================================

    private void recalculateStatuses(int entryId) {
        try (Connection con = DBConnection.getConnection()) {

            // Step 1: get qty_ordered
            PreparedStatement psOrd = con.prepareStatement(
                "SELECT qty_ordered FROM order_table WHERE entry_id = ?");
            psOrd.setInt(1, entryId);
            ResultSet rsOrd = psOrd.executeQuery();
            if (!rsOrd.next()) return;
            int qtyOrdered = rsOrd.getInt("qty_ordered");

            // Step 2: sum of remaining ACTIVE bills only
            PreparedStatement psSum = con.prepareStatement(
                "SELECT IFNULL(SUM(qty_received), 0) AS total " +
                "FROM bill_invoice " +
                "WHERE entry_id = ? AND record_status = 'ACTIVE'");
            psSum.setInt(1, entryId);
            ResultSet rsSum = psSum.executeQuery();
            rsSum.next();
            int totalReceived = rsSum.getInt("total");

            // Step 3: derive statuses in Java
            String newOrderStatus = (totalReceived >= qtyOrdered) ? "PAID"     : "IN_PROCESS";
            String newBillStatus  = (totalReceived >= qtyOrdered) ? "COMPLETE" : "INCOMPLETE";

            // Step 4: plain UPDATE order_status
            PreparedStatement psO = con.prepareStatement(
                "UPDATE order_table SET order_status = ? WHERE entry_id = ?");
            psO.setString(1, newOrderStatus);
            psO.setInt(2, entryId);
            psO.executeUpdate();

            // Step 5: plain UPDATE bill_status on all remaining ACTIVE bills
            PreparedStatement psB = con.prepareStatement(
                "UPDATE bill_invoice SET bill_status = ? " +
                "WHERE entry_id = ? AND record_status = 'ACTIVE'");
            psB.setString(1, newBillStatus);
            psB.setInt(2, entryId);
            psB.executeUpdate();

        } catch (Exception e) { showError(e.getMessage()); }
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    private <T> TableColumn<T,String> col(String title, double width) {
        TableColumn<T,String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        return c;
    }

    private <T> TableColumn<T,Integer> colInt(String title, double width) {
        TableColumn<T,Integer> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        return c;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        l.setStyle("-fx-text-fill:#3E2723;");
        return l;
    }

    private TextField field() {
        TextField tf = new TextField();
        tf.setPrefSize(260, 42);
        tf.setStyle(FIELD_STYLE);
        return tf;
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(20); g.setVgap(15);
        g.setPadding(new Insets(20));
        g.setStyle("-fx-background-color:#F5DEB3;");
        return g;
    }

    private boolean confirmDelete(String what) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete " + what + "?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Delete");
        alert.getDialogPane().setStyle("-fx-background-color:#F5DEB3;");
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }

    private String generateNextProductId() {
        String sql = "SELECT pid FROM product ORDER BY pid DESC LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String lastId = rs.getString("pid"); // e.g., P0005
                int num = Integer.parseInt(lastId.substring(1));
                num++;
                return String.format("P%04d", num);
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
        return "P0001";
    }

    public Scene getScene() {
        return scene;
    }
}
