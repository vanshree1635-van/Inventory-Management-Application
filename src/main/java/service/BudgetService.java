package service;

import model.BudgetData;
import java.util.HashMap;
import java.util.Map;

public class BudgetService {

    public BudgetData getBudgetAnalysis() {

        double totalBudget = 500000;

        Map<String, Double> distribution = new HashMap<>();

        distribution.put("Stationery", 120000.0);
        distribution.put("Furniture", 150000.0);
        distribution.put("Miscellaneous Items", 80000.0);
        distribution.put("Computer Sets", 100000.0);

        double amountSpent = distribution.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        double remaining = totalBudget - amountSpent;

        return new BudgetData(totalBudget, amountSpent, remaining, distribution);
    }
}
