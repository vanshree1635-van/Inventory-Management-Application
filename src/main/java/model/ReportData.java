package model;

public class ReportData {
    public int purchase;
    public int issued;

    public int returnToInventory;
    public int returnToSupplier;
    public int returnFromSupplier;

    // ✅ FINAL STOCK LOGIC
    public int getStock() {
        return purchase
                - issued
                - returnToSupplier
                + returnToInventory
                + returnFromSupplier;
    }

    // ✅ MERGED RETURN (WHAT YOU WANT)
    public int getEffectiveReturn() {
        return returnToInventory + returnFromSupplier;
    }
}
