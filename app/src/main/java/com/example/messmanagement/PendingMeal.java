package com.example.messmanagement;

public class PendingMeal {
    public String memberName;
    public String date;
    public int breakfast;
    public int lunch;
    public int dinner;

    // ✅ what changed last time (so we notify exactly that)
    public String changedMealType; // "Breakfast" / "Lunch" / "Dinner"
    public int changedValue;       // 1 (add) or 0 (cancel)
}
