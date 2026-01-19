package com.example.messmanagement;

public class ExpenseBreakdown {
    public double mealCost;
    public double otherExpensesShare;
    public double paidAmount;
    public double totalCost;
    public double balance;
    public int breakfastCount;
    public int lunchCount;
    public int dinnerCount;

    public ExpenseBreakdown() {
        this.mealCost = 0.0;
        this.otherExpensesShare = 0.0;
        this.paidAmount = 0.0;
        this.totalCost = 0.0;
        this.balance = 0.0;
        this.breakfastCount = 0;
        this.lunchCount = 0;
        this.dinnerCount = 0;
    }
}