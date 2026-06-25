package model;

import java.util.Map;

public class BudgetData {

    private final double totalBudget;
    private final double amountSpent;
    private final double remainingBalance;
    private final Map<String, Double> categoryDistribution;

    public BudgetData(double totalBudget,
                      double amountSpent,
                      double remainingBalance,
                      Map<String, Double> categoryDistribution) {

        this.totalBudget = totalBudget;
        this.amountSpent = amountSpent;
        this.remainingBalance = remainingBalance;
        this.categoryDistribution = categoryDistribution;
    }

    public double getTotalBudget() {
        return totalBudget;
    }

    public double getAmountSpent() {
        return amountSpent;
    }

    public double getRemainingBalance() {
        return remainingBalance;
    }

    public Map<String, Double> getCategoryDistribution() {
        return categoryDistribution;
    }
}
