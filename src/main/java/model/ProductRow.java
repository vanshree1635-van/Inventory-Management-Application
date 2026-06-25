package model;

import javafx.beans.property.*;

public class ProductRow {

    private final StringProperty pid;
    private final StringProperty name;
    private final IntegerProperty qty;
    private final StringProperty ptype;

    public ProductRow(String pid, String name, int qty, String ptype) {
        this.pid = new SimpleStringProperty(pid);
        this.name = new SimpleStringProperty(name);
        this.qty = new SimpleIntegerProperty(qty);
        this.ptype = new SimpleStringProperty(ptype);
    }
    
    public ProductRow(String pid, String name, int qty) {
        this.pid = new SimpleStringProperty(pid);
        this.name = new SimpleStringProperty(name);
        this.qty = new SimpleIntegerProperty(qty);
        this.ptype = new SimpleStringProperty(""); // empty for now
    }

    public StringProperty pidProperty() { return pid; }
    public StringProperty nameProperty() { return name; }
    public IntegerProperty qtyProperty() { return qty; }
    public StringProperty ptypeProperty() { return ptype; }

    public String getPid() { return pid.get(); }
    public String getName() { return name.get(); }
    public int getQty() { return qty.get(); }
    public void setName(String name) {
        this.name.set(name);
    }
    public void setQty(int qty) {
        this.qty.set(qty);
    }
}