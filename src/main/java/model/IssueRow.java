package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class IssueRow {

    private final IntegerProperty issueId;
    private final StringProperty pid;
    private final StringProperty issueTo;
    private final StringProperty dept;
    private final IntegerProperty qtyIssued;
    private final IntegerProperty qtyReturn = new SimpleIntegerProperty(0);
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public IssueRow(int id,String pid,String to,String by,String dept,int qty,String date){
        this.issueId=new SimpleIntegerProperty(id);
        this.pid=new SimpleStringProperty(pid);
        this.issueTo=new SimpleStringProperty(to);
        this.dept=new SimpleStringProperty(dept);
        this.qtyIssued=new SimpleIntegerProperty(qty);
    }

    public int getIssueId(){return issueId.get();}
    public String getPid(){return pid.get();}
    public int getQtyIssued(){return qtyIssued.get();}
    public int getQtyReturn(){return qtyReturn.get();}
    public void setQtyReturn(int q){qtyReturn.set(q);}
    public boolean isSelected(){return selected.get();}

    public IntegerProperty issueIdProperty(){return issueId;}
    public StringProperty pidProperty(){return pid;}
    public StringProperty issueToProperty(){return issueTo;}
    public StringProperty deptProperty(){return dept;}
    public IntegerProperty qtyIssuedProperty(){return qtyIssued;}
    public IntegerProperty qtyReturnProperty(){return qtyReturn;}
    public BooleanProperty selectedProperty(){return selected;}
}