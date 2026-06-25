package model;

public class ReportData {
    public int purchase;
    public int issued;
    public int returned;

    public int getPending() {
        return purchase - issued + returned;
    }
}