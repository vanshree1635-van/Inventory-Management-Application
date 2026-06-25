package model;

import javafx.beans.property.*;

public class OrderRow {

    private BooleanProperty selected;
    private StringProperty orderNo;
    private StringProperty productName;
    private StringProperty supplierName;
    private StringProperty orderDate;
    private IntegerProperty qtyReceived;
    private StringProperty status;

    public OrderRow(boolean selected,
                    String orderNo,
                    String productName,
                    String supplierName,
                    String orderDate,
                    int qtyReceived,
                    String status) {

        this.selected = new SimpleBooleanProperty(selected);
        this.orderNo = new SimpleStringProperty(orderNo);
        this.productName = new SimpleStringProperty(productName);
        this.supplierName = new SimpleStringProperty(supplierName);
        this.orderDate = new SimpleStringProperty(orderDate);
        this.qtyReceived = new SimpleIntegerProperty(qtyReceived);
        this.status = new SimpleStringProperty(status);
    }

    // ================= PROPERTIES =================

    public BooleanProperty selectedProperty() { return selected; }
    public StringProperty orderNoProperty() { return orderNo; }
    public StringProperty productNameProperty() { return productName; }
    public StringProperty supplierNameProperty() { return supplierName; }
    public StringProperty orderDateProperty() { return orderDate; }
    public IntegerProperty qtyReceivedProperty() { return qtyReceived; }
    public StringProperty statusProperty() { return status; }

    // ================= GETTERS =================

    public boolean isSelected() { return selected.get(); }
    public String getOrderNo() { return orderNo.get(); }
}