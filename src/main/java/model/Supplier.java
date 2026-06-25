package model;

import javafx.beans.property.*;

public class Supplier {

    private final StringProperty name;
    private final StringProperty contact; // maps to DB column contact_no
    private final StringProperty address;
    private final StringProperty ptypeId; // Product Type ID as string

    // -------------------- Constructor --------------------
    public Supplier(String name, String contact, String address, String ptypeId) {
        this.name = new SimpleStringProperty(name);
        this.contact = new SimpleStringProperty(contact);
        this.address = new SimpleStringProperty(address);
        this.ptypeId = new SimpleStringProperty(ptypeId);
    }

    // -------------------- JavaFX Properties --------------------
    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty contactProperty() {
        return contact;
    }

    public StringProperty addressProperty() {
        return address;
    }

    public StringProperty ptypeIdProperty() {
        return ptypeId;
    }

    // -------------------- Standard Getters --------------------
    public String getName() {
        return name.get();
    }

    public String getContact() {
        return contact.get();
    }

    public String getAddress() {
        return address.get();
    }

    public String getPtypeId() {
        return ptypeId.get();
    }

    // -------------------- Standard Setters --------------------
    public void setName(String value) {
        name.set(value);
    }

    public void setContact(String value) {
        contact.set(value);
    }

    public void setAddress(String value) {
        address.set(value);
    }

    public void setPtypeId(String value) {
        ptypeId.set(value);
    }
}