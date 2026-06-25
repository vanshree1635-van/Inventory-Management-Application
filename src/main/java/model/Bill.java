
package model;

import javafx.beans.property.*;

public class Bill {

    private final StringProperty billNo;
    private final DoubleProperty amount;

    public Bill(String billNo, double amount) {
        this.billNo = new SimpleStringProperty(billNo);
        this.amount = new SimpleDoubleProperty(amount);
    }

    public StringProperty billNoProperty() {
        return billNo;
    }

    public DoubleProperty amountProperty() {
        return amount;
    }
}