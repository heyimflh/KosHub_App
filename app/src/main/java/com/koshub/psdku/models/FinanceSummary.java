package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for Finance Summary statistics.
 */
public class FinanceSummary implements Serializable {
    private double totalIncome;
    private double availableBalance;
    private double pendingBalance;
    private double totalWithdrawn;
    private double totalPendingWithdraw;
    private int transactionCount;
    private int withdrawalCount;

    public FinanceSummary() {
    }

    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    public double getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(double availableBalance) { this.availableBalance = availableBalance; }

    public double getPendingBalance() { return pendingBalance; }
    public void setPendingBalance(double pendingBalance) { this.pendingBalance = pendingBalance; }

    public double getTotalWithdrawn() { return totalWithdrawn; }
    public void setTotalWithdrawn(double totalWithdrawn) { this.totalWithdrawn = totalWithdrawn; }

    public double getTotalPendingWithdraw() { return totalPendingWithdraw; }
    public void setTotalPendingWithdraw(double totalPendingWithdraw) { this.totalPendingWithdraw = totalPendingWithdraw; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public int getWithdrawalCount() { return withdrawalCount; }
    public void setWithdrawalCount(int withdrawalCount) { this.withdrawalCount = withdrawalCount; }
}
