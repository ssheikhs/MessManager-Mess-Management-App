package com.example.messmanagement;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlySummaryActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvMonthTitle, tvTotalMeals, tvTotalOtherExpenses, tvMealRate;
    private ListView lvSummary;

    private MessDBHelper messDb;
    private ArrayList<Summary> summaryList;
    private SummaryAdapter summaryAdapter;
    private List<Member> allMembers;

    private FirebaseFirestore fs;
    private ListenerRegistration expensesListener;
    private ListenerRegistration mealsListener;

    private final Map<String, MealCount> mealCountMap = new HashMap<>();
    private final Map<String, Double> otherExpenseMap = new HashMap<>();
    private final Map<String, Double> paymentMap = new HashMap<>();
    private double totalOtherExpensesAll = 0.0;

    private String monthPrefix; // yyyy-MM
    private String monthName;   // MMMM yyyy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_summary);

        messDb = new MessDBHelper(this);
        fs = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);
        tvMonthTitle = findViewById(R.id.tvMonthTitle);
        tvTotalMeals = findViewById(R.id.tvTotalMeals);
        tvTotalOtherExpenses = findViewById(R.id.tvTotalOtherExpenses);
        tvMealRate = findViewById(R.id.tvMealRate);
        lvSummary = findViewById(R.id.lvSummary);

        ivBack.setOnClickListener(v -> onBackPressed());

        monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
        tvMonthTitle.setText("Monthly Summary — " + monthName);

        summaryList = new ArrayList<>();
        summaryAdapter = new SummaryAdapter(this, summaryList);
        lvSummary.setAdapter(summaryAdapter);

        setupMealRateText();
    }

    private void setupMealRateText() {
        double[] mealPrices = messDb.getCurrentMealPrices();
        tvMealRate.setText(String.format(Locale.getDefault(),
                "Meal prices — Breakfast: %.0f ৳, Lunch: %.0f ৳, Dinner: %.0f ৳",
                mealPrices[0], mealPrices[1], mealPrices[2]));
    }

    // ---------------- Firestore MEALS ----------------

    private void attachMealsListener() {
        if (mealsListener != null) {
            mealsListener.remove();
            mealsListener = null;
        }

        mealsListener = fs.collection("meals_daily")
                .addSnapshotListener((@Nullable QuerySnapshot value,
                                      @Nullable FirebaseFirestoreException error) -> {
                    if (error != null || value == null) {
                        Toast.makeText(MonthlySummaryActivity.this,
                                "Online meals unavailable — showing offline data.",
                                Toast.LENGTH_LONG).show();
                        buildSummaryFromLocalFallback();
                        return;
                    }

                    mealCountMap.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String date = doc.getString("date");
                        if (date == null || !date.startsWith(monthPrefix)) continue;

                        String memberName = doc.getString("memberName");
                        if (memberName == null) continue;

                        Long bL = doc.getLong("breakfast");
                        Long lL = doc.getLong("lunch");
                        Long dL = doc.getLong("dinner");
                        int b = (bL == null) ? 0 : bL.intValue();
                        int l = (lL == null) ? 0 : lL.intValue();
                        int d = (dL == null) ? 0 : dL.intValue();

                        MealCount mc = mealCountMap.get(memberName);
                        if (mc == null) {
                            mc = new MealCount();
                            mealCountMap.put(memberName, mc);
                        }
                        mc.breakfast += b;
                        mc.lunch += l;
                        mc.dinner += d;

                        // sync to local for offline fallback
                        messDb.upsertMealsFromRemote(memberName, date, b, l, d);
                    }

                    rebuildSummaryUI();
                });
    }

    // ---------------- Firestore EXPENSES ----------------

    private void attachExpensesListener() {
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }

        expensesListener = fs.collection("expenses")
                .addSnapshotListener((@Nullable QuerySnapshot value,
                                      @Nullable FirebaseFirestoreException error) -> {
                    if (error != null || value == null) {
                        Toast.makeText(MonthlySummaryActivity.this,
                                "Online expenses unavailable — showing offline data.",
                                Toast.LENGTH_LONG).show();
                        buildSummaryFromLocalFallback();
                        return;
                    }

                    otherExpenseMap.clear();
                    paymentMap.clear();
                    totalOtherExpensesAll = 0.0;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String remoteId = doc.getId(); // ✅ required by MessDBHelper signature

                        String date = doc.getString("date");
                        if (date == null || !date.startsWith(monthPrefix)) continue;

                        String memberName = doc.getString("paidBy");
                        if (memberName == null) memberName = "Unknown";

                        String title = doc.getString("title");
                        String category = doc.getString("category");
                        if (category == null) category = "";

                        Double amount = doc.getDouble("amount");
                        if (amount == null) amount = 0.0;

                        // ✅ FIX: pass 6 params (remoteId, date, title, category, paidBy, amount)
                        messDb.upsertExpenseFromRemote(
                                remoteId,
                                date,
                                title != null ? title : "",
                                category,
                                memberName,
                                amount
                        );

                        if (MessDBHelper.CATEGORY_PAYMENT.equals(category)) {
                            double prev = paymentMap.containsKey(memberName) ? paymentMap.get(memberName) : 0.0;
                            paymentMap.put(memberName, prev + amount);
                        } else {
                            double prev = otherExpenseMap.containsKey(memberName) ? otherExpenseMap.get(memberName) : 0.0;
                            otherExpenseMap.put(memberName, prev + amount);
                            totalOtherExpensesAll += amount;
                        }
                    }

                    rebuildSummaryUI();
                });
    }

    // ---------------- Combine meals + expenses (Firestore path) ----------------

    private void rebuildSummaryUI() {
        allMembers = messDb.getAllMembers();
        if (allMembers == null) allMembers = new ArrayList<>();

        summaryList.clear();

        double[] prices = messDb.getCurrentMealPrices();
        int totalMealsAll = 0;

        for (Member m : allMembers) {
            String name = m.name;

            MealCount mc = mealCountMap.get(name);
            int b = (mc == null) ? 0 : mc.breakfast;
            int l = (mc == null) ? 0 : mc.lunch;
            int d = (mc == null) ? 0 : mc.dinner;

            int memberTotalMeals = b + l + d;
            totalMealsAll += memberTotalMeals;

            double mealCost = b * prices[0] + l * prices[1] + d * prices[2];

            double otherExp = otherExpenseMap.containsKey(name) ? otherExpenseMap.get(name) : 0.0;
            double paid = paymentMap.containsKey(name) ? paymentMap.get(name) : 0.0;

            double totalCost = mealCost + otherExp;
            double balance = totalCost - paid;

            summaryList.add(new Summary(
                    name, b, l, d,
                    mealCost, otherExp, paid,
                    totalCost, balance
            ));
        }

        tvTotalMeals.setText("Total meals (all members): " + totalMealsAll);
        tvTotalOtherExpenses.setText(String.format(Locale.getDefault(),
                "Total other expenses: %.0f ৳", totalOtherExpensesAll));

        summaryAdapter.notifyDataSetChanged();
    }

    // ---------------- Local-only fallback (offline) ----------------

    private void buildSummaryFromLocalFallback() {
        allMembers = messDb.getAllMembers();
        if (allMembers == null) allMembers = new ArrayList<>();

        summaryList.clear();

        double[] prices = messDb.getCurrentMealPrices();
        int totalMealsAll = 0;

        double totalOtherLocal = messDb.getTotalLocalOtherExpensesForMonth(monthPrefix);

        for (Member m : allMembers) {
            String name = m.name;

            int[] counts = messDb.getMonthlyMealCountsForMember(name, monthPrefix);
            int b = counts.length > 0 ? counts[0] : 0;
            int l = counts.length > 1 ? counts[1] : 0;
            int d = counts.length > 2 ? counts[2] : 0;

            totalMealsAll += (b + l + d);

            double mealCost = b * prices[0] + l * prices[1] + d * prices[2];

            double[] ep = messDb.getMemberLocalExpenseAndPaymentForMonth(name, monthPrefix);
            double otherExp = ep[0];
            double paid = ep[1];

            double totalCost = mealCost + otherExp;
            double balance = totalCost - paid;

            summaryList.add(new Summary(
                    name, b, l, d,
                    mealCost, otherExp, paid,
                    totalCost, balance
            ));
        }

        tvTotalMeals.setText("Total meals (all members): " + totalMealsAll);
        tvTotalOtherExpenses.setText(String.format(Locale.getDefault(),
                "Total other expenses: %.0f ৳", totalOtherLocal));

        summaryAdapter.notifyDataSetChanged();
    }

    private static class MealCount {
        int breakfast = 0;
        int lunch = 0;
        int dinner = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachMealsListener();
        attachExpensesListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mealsListener != null) {
            mealsListener.remove();
            mealsListener = null;
        }
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }
    }
}
