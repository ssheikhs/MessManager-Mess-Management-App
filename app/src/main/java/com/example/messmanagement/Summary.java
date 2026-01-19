package com.example.messmanagement;

public class Summary {
    public String memberName;
    public int breakfast;
    public int lunch;
    public int dinner;
    public double mealCost;
    public double otherShare;
    public double paid;
    public double totalCost;
    public double balance;

    public Summary(String memberName,
                   int breakfast,
                   int lunch,
                   int dinner,
                   double mealCost,
                   double otherShare,
                   double paid,
                   double totalCost,
                   double balance) {
        this.memberName = memberName;
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
        this.mealCost = mealCost;
        this.otherShare = otherShare;
        this.paid = paid;
        this.totalCost = totalCost;
        this.balance = balance;
    }
}
