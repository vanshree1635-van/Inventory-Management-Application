package model;

import java.util.Map;

public class BudgetData {

    private final double totalBudget;
    private final double amountSpent;
    private final double remainingBalance;
    private final Map<String, Double> categoryDistribution;

    private final double prevTotalBudget;
    private final double prevAmountSpent;
    private final double prevRemainingBalance;
    private final Map<String, Double> prevCategoryDistribution;

    public BudgetData(double totalBudget,
                      double amountSpent,
                      double remainingBalance,
                      Map<String, Double> categoryDistribution,
                      double prevTotalBudget,
                      double prevAmountSpent,
                      double prevRemainingBalance,
                      Map<String, Double> prevCategoryDistribution) {

        this.totalBudget              = totalBudget;
        this.amountSpent              = amountSpent;
        this.remainingBalance         = remainingBalance;
        this.categoryDistribution     = categoryDistribution;
        this.prevTotalBudget          = prevTotalBudget;
        this.prevAmountSpent          = prevAmountSpent;
        this.prevRemainingBalance     = prevRemainingBalance;
        this.prevCategoryDistribution = prevCategoryDistribution;
    }

    public double getTotalBudget()                          { return totalBudget; }
    public double getAmountSpent()                          { return amountSpent; }
    public double getRemainingBalance()                     { return remainingBalance; }
    public Map<String, Double> getCategoryDistribution()    { return categoryDistribution; }

    public double getPrevTotalBudget()                      { return prevTotalBudget; }
    public double getPrevAmountSpent()                      { return prevAmountSpent; }
    public double getPrevRemainingBalance()                 { return prevRemainingBalance; }
    public Map<String, Double> getPrevCategoryDistribution(){ return prevCategoryDistribution; }
}
