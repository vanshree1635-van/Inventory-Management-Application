package model;

import javafx.beans.property.*;

public class IssueModel {

    private IntegerProperty issueId;
    private StringProperty pid;
    private IntegerProperty qty;
    private BooleanProperty selected = new SimpleBooleanProperty(false);

    public IssueModel(int issueId, String pid, int qty) {
        this.issueId = new SimpleIntegerProperty(issueId);
        this.pid = new SimpleStringProperty(pid);
        this.qty = new SimpleIntegerProperty(qty);
    }

    public int getIssueId() { return issueId.get(); }
    public String getPid() { return pid.get(); }
    public int getQty() { return qty.get(); }

    public IntegerProperty issueIdProperty() { return issueId; }
    public StringProperty pidProperty() { return pid; }
    public IntegerProperty qtyProperty() { return qty; }

    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
}