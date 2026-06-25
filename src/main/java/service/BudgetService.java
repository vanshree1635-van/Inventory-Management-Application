package service;

import model.BudgetData;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class BudgetService {

    public BudgetData getBudgetAnalysis() {

        double totalBudget     = 0, amountSpent     = 0;
        double prevTotalBudget = 0, prevAmountSpent = 0;
        Map<String, Double> distribution     = new HashMap<>();
        Map<String, Double> prevDistribution = new HashMap<>();

        // ── Date ranges ──────────────────────────────────────────────────────
        LocalDate now      = LocalDate.now();
        String currStart   = now.withDayOfMonth(1).toString();
        String currEnd     = now.withDayOfMonth(now.lengthOfMonth()).toString();

        LocalDate prevMonth = now.minusMonths(1);
        String prevStart   = prevMonth.withDayOfMonth(1).toString();
        String prevEnd     = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).toString();

        try (Connection con = db.DBConnection.getConnection()) {

            // ════════════════════════════════════════════════════════════════
            //  TOTAL BUDGET = order_table portion + bill_invoice portion
            //
            //  order_table portion  : qty_ordered × (SUM(bill_amount) / SUM(qty_received))
            //                         bills aggregated per entry_id to avoid double-counting
            //                         when multiple bills exist for the same order
            //                         filtered by order_date
            //
            //  bill_invoice portion : bill_amount where entry_id IS NULL
            //                         (standalone bills not tied to an order)
            //                         filtered by received_date, ACTIVE
            // ════════════════════════════════════════════════════════════════

            // -- order_table portion (bills aggregated per entry to avoid double-count) --
            String orderBudgetSql = """
                SELECT COALESCE(
                    SUM(o.qty_ordered * (b.total_bill / b.total_qty_received)), 0
                )
                FROM order_table o
                JOIN (
                    SELECT entry_id,
                           SUM(bill_amount)   AS total_bill,
                           SUM(qty_received)  AS total_qty_received
                    FROM bill_invoice
                    WHERE record_status = 'ACTIVE'
                    GROUP BY entry_id
                ) b ON b.entry_id = o.entry_id
                WHERE o.record_status = 'ACTIVE'
                  AND o.order_date BETWEEN ? AND ?
            """;

            // -- standalone bill_invoice portion (entry_id IS NULL) --
            String standaloneBillSql = """
                SELECT COALESCE(SUM(bill_amount), 0)
                FROM bill_invoice
                WHERE record_status = 'ACTIVE'
                  AND entry_id IS NULL
                  AND received_date BETWEEN ? AND ?
            """;

            // Current month — order portion
            try (PreparedStatement ps = con.prepareStatement(orderBudgetSql)) {
                ps.setString(1, currStart);
                ps.setString(2, currEnd);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) totalBudget += rs.getDouble(1);
            }

            // Current month — standalone bill portion
            try (PreparedStatement ps = con.prepareStatement(standaloneBillSql)) {
                ps.setString(1, currStart);
                ps.setString(2, currEnd);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) totalBudget += rs.getDouble(1);
            }

            // Previous month — order portion
            try (PreparedStatement ps = con.prepareStatement(orderBudgetSql)) {
                ps.setString(1, prevStart);
                ps.setString(2, prevEnd);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) prevTotalBudget += rs.getDouble(1);
            }

            // Previous month — standalone bill portion
            try (PreparedStatement ps = con.prepareStatement(standaloneBillSql)) {
                ps.setString(1, prevStart);
                ps.setString(2, prevEnd);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) prevTotalBudget += rs.getDouble(1);
            }

            // ════════════════════════════════════════════════════════════════
            //  AMOUNT SPENT = bill_invoice only (ACTIVE), filtered by
            //                 received_date — all bills regardless of entry_id
            // ════════════════════════════════════════════════════════════════

            String spentSql = """
                SELECT COALESCE(SUM(bill_amount), 0)
                FROM bill_invoice
                WHERE record_status = 'ACTIVE'
                  AND received_date BETWEEN ? AND ?
            """;

            // Current month spent
            try (PreparedStatement ps = con.prepareStatement(spentSql)) {
                ps.setString(1, currStart);
                ps.setString(2, currEnd);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) amountSpent = rs.getDouble(1);
            }

            // Previous month spent
            try (PreparedStatement ps = con.prepareStatement(spentSql)) {
                ps.setString(1, prevStart);
                ps.setString(2, prevEnd);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) prevAmountSpent = rs.getDouble(1);
            }

            // ════════════════════════════════════════════════════════════════
            //  CATEGORY DISTRIBUTION — order_table × bill_invoice, ACTIVE
            //  grouped by product_type
            //  bills aggregated per entry_id to avoid double-counting
            // ════════════════════════════════════════════════════════════════

            String categorySql = """
                SELECT pt.ptype_name,
                       COALESCE(SUM(o.qty_ordered * (b.total_bill / b.total_qty_received)), 0)
                FROM order_table o
                JOIN (
                    SELECT entry_id,
                           SUM(bill_amount)   AS total_bill,
                           SUM(qty_received)  AS total_qty_received
                    FROM bill_invoice
                    WHERE record_status = 'ACTIVE'
                    GROUP BY entry_id
                ) b ON b.entry_id = o.entry_id
                JOIN product p        ON o.pid        = p.pid
                JOIN product_type pt  ON p.ptype_id   = pt.ptype_id
                WHERE o.record_status = 'ACTIVE'
                  AND o.order_date BETWEEN ? AND ?
                GROUP BY pt.ptype_name
            """;

            // Current month distribution
            try (PreparedStatement ps = con.prepareStatement(categorySql)) {
                ps.setString(1, currStart);
                ps.setString(2, currEnd);
                ResultSet rs = ps.executeQuery();
                while (rs.next())
                    distribution.put(rs.getString(1), rs.getDouble(2));
            }

            // Previous month distribution
            try (PreparedStatement ps = con.prepareStatement(categorySql)) {
                ps.setString(1, prevStart);
                ps.setString(2, prevEnd);
                ResultSet rs = ps.executeQuery();
                while (rs.next())
                    prevDistribution.put(rs.getString(1), rs.getDouble(2));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // REMAINING = totalBudget − amountSpent
        return new BudgetData(
            totalBudget,      amountSpent,      totalBudget - amountSpent,      distribution,
            prevTotalBudget,  prevAmountSpent,  prevTotalBudget - prevAmountSpent, prevDistribution
        );
    }
}
