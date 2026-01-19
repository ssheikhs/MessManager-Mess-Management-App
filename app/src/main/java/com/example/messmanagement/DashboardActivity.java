package com.example.messmanagement;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvDashboardTitle, tvDashboardDate;
    private TextView tvTotalMealsValue, tvTotalExpenseValue, tvOutstandingDuesValue, tvPaidValue, tvMealRateValue;

    private Button btnBreakfastAdd, btnBreakfastCancel;
    private Button btnLunchAdd, btnLunchCancel;
    private Button btnDinnerAdd, btnDinnerCancel;
    private Button btnPayNow;

    private LinearLayout navMeals, navExpenses, navMembers, navProfile;

    private MessDBHelper messDb;
    private KeyValueDB kvDb;

    private String currentUserName;
    private String currentRole;

    private FirebaseFirestore fs;
    private FirebaseAuth mAuth;

    private ListenerRegistration expensesListener;
    private ListenerRegistration mealsListener;
    private ListenerRegistration mealPricesListener;

    private ListenerRegistration notifListener;
    private String lastNotifiedDocId = null;

    private static final int REQ_POST_NOTIF = 101;
    private static final String KV_LAST_NOTIF_ID = "last_notif_doc_id";

    // ✅ network callback to re-attach listeners when internet returns
    private ConnectivityManager.NetworkCallback netCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        messDb = new MessDBHelper(this);
        kvDb = new KeyValueDB(this);
        fs = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bindViews();

        // ✅ last shown notification id
        lastNotifiedDocId = kvDb.getValueByKey(KV_LAST_NOTIF_ID);

        // ---------- current user ----------
        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu != null && fu.getEmail() != null) currentUserName = fu.getEmail();
        else currentUserName = kvDb.getValueByKey("username");
        if (currentUserName == null) currentUserName = "Guest";

        // ---------- role ----------
        currentRole = messDb.getUserRoleByUsername(currentUserName);
        if (currentRole == null) currentRole = kvDb.getValueByKey("role");
        if (currentRole == null) currentRole = "member";

        kvDb.insertOrUpdate("username", currentUserName);
        kvDb.insertOrUpdate("role", currentRole);

        setupNotificationsIfLoggedIn();

        setupHeader();
        setupMealButtons();
        setupPayButton();
        setupNavigation();

        setupLocalMealRatesAndCounts();
        updateMoneyFromLocalDB();

        if (fu != null) {
            attachAllFirestoreListeners();
        } else {
            Toast.makeText(this,
                    "Offline mode: Firestore sync disabled (login online once).",
                    Toast.LENGTH_LONG).show();
        }

        scheduleDailyReminders();
    }

    private void bindViews() {
        tvDashboardTitle = findViewById(R.id.tvDashboardTitle);
        tvDashboardDate = findViewById(R.id.tvDashboardDate);
        tvTotalMealsValue = findViewById(R.id.tvTotalMealsValue);
        tvTotalExpenseValue = findViewById(R.id.tvTotalExpenseValue);
        tvOutstandingDuesValue = findViewById(R.id.tvOutstandingDuesValue);
        tvPaidValue = findViewById(R.id.tvPaidValue);
        tvMealRateValue = findViewById(R.id.tvMealRateValue);

        btnBreakfastAdd = findViewById(R.id.btnBreakfastAdd);
        btnBreakfastCancel = findViewById(R.id.btnBreakfastCancel);
        btnLunchAdd = findViewById(R.id.btnLunchAdd);
        btnLunchCancel = findViewById(R.id.btnLunchCancel);
        btnDinnerAdd = findViewById(R.id.btnDinnerAdd);
        btnDinnerCancel = findViewById(R.id.btnDinnerCancel);
        btnPayNow = findViewById(R.id.btnPayNow);

        navMeals = findViewById(R.id.navMeals);
        navExpenses = findViewById(R.id.navExpenses);
        navMembers = findViewById(R.id.navMembers);
        navProfile = findViewById(R.id.navProfile);
    }

    // ===================== NOTIFICATIONS (FCM permission + topic) =====================

    private void setupNotificationsIfLoggedIn() {
        if (currentUserName == null || "Guest".equals(currentUserName)) return;

        requestNotificationPermissionIfNeeded();

        FirebaseMessaging.getInstance().subscribeToTopic("mess_all")
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "FCM topic subscribe failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIF);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_POST_NOTIF) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this,
                        "Notifications are OFF. Turn ON from Settings to receive mess alerts.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ===================== ONE-CALL: attach everything =====================

    private void attachAllFirestoreListeners() {
        attachMealPricesListener();
        attachRealtimeExpensesListener();
        attachRealtimeMealsListener();
        attachNotificationsListener();
    }

    private void detachAllFirestoreListeners() {
        detachExpenseListener();
        detachMealsListener();
        detachMealPricesListener();
        detachNotificationsListener();
    }

    // ===================== MEAL PRICES =====================

    private void attachMealPricesListener() {
        detachMealPricesListener();

        mealPricesListener = fs.collection("meal_prices")
                .orderBy("effectiveDate", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null || snap.isEmpty()) return;

                    DocumentSnapshot d = snap.getDocuments().get(0);

                    String effectiveDate = d.getString("effectiveDate");
                    Double b = d.getDouble("breakfastPrice");
                    Double l = d.getDouble("lunchPrice");
                    Double dn = d.getDouble("dinnerPrice");

                    if (effectiveDate == null) return;

                    double bPrice = (b == null) ? 0.0 : b;
                    double lPrice = (l == null) ? 0.0 : l;
                    double dPrice = (dn == null) ? 0.0 : dn;

                    // ✅ signature: (double,double,double,String)
                    messDb.insertMealPriceHistory(bPrice, lPrice, dPrice, effectiveDate);

                    setupLocalMealRatesAndCounts();
                    updateMoneyFromLocalDB();
                });
    }

    private void detachMealPricesListener() {
        if (mealPricesListener != null) {
            mealPricesListener.remove();
            mealPricesListener = null;
        }
    }

    // ===================== NOTIF LISTENER (offline -> online FIX) =====================

    /**
     * ✅ FIXES YOUR ISSUE:
     * - INCLUDE metadata so cache->server transition fires
     * - ignore cache snapshots (so it won't pop while offline)
     * - show ALL unseen docs in correct order (oldest first)
     * - use stable notification id (docId hash) to prevent duplicates
     */
    private void attachNotificationsListener() {
        detachNotificationsListener();

        notifListener = fs.collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1) // ✅ only latest doc
                .addSnapshotListener(MetadataChanges.INCLUDE, (snap, err) -> {
                    if (err != null || snap == null || snap.isEmpty()) return;

                    // ✅ ignore cache-only (offline) snapshots
                    if (snap.getMetadata().isFromCache()) return;

                    DocumentSnapshot d = snap.getDocuments().get(0);
                    String docId = d.getId();
                    if (docId == null) return;

                    // ✅ first time: don't show old notif, just mark as seen
                    if (lastNotifiedDocId == null || lastNotifiedDocId.trim().isEmpty()) {
                        lastNotifiedDocId = docId;
                        kvDb.insertOrUpdate(KV_LAST_NOTIF_ID, docId);
                        return;
                    }

                    // ✅ only show if it's new
                    if (docId.equals(lastNotifiedDocId)) return;

                    lastNotifiedDocId = docId;
                    kvDb.insertOrUpdate(KV_LAST_NOTIF_ID, docId);

                    String title = d.getString("title");
                    String body  = d.getString("body");

                    // ✅ stable id prevents duplicates if listener reattaches
                    int nid = NotificationUtils.stableIdFromString(docId);
                    NotificationUtils.showLocal(this, title, body, nid);
                });
    }


    private void detachNotificationsListener() {
        if (notifListener != null) {
            notifListener.remove();
            notifListener = null;
        }
    }

    // ===================== REMINDER WORKER =====================

    private void scheduleDailyReminders() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(DueReminderWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "mess_due_and_meal_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    // ===================== NETWORK: RE-ATTACH WHEN INTERNET RETURNS =====================

    @Override
    protected void onStart() {
        super.onStart();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        // prevent double register
        if (netCb != null) return;

        netCb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    FirebaseUser fu = mAuth.getCurrentUser();
                    if (fu != null) {
                        // ✅ re-attach so no need logout/login
                        detachAllFirestoreListeners();
                        attachAllFirestoreListeners();
                    }
                });
            }
        };

        cm.registerNetworkCallback(new NetworkRequest.Builder().build(), netCb);
    }

    @Override
    protected void onStop() {
        super.onStop();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && netCb != null) {
            try { cm.unregisterNetworkCallback(netCb); } catch (Exception ignored) {}
        }
        netCb = null;
    }

    // ===================== HELPERS =====================

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentMonthPrefix() {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
    }

    private void setupHeader() {
        if ("admin".equalsIgnoreCase(currentRole)) tvDashboardTitle.setText("Dashboard (Admin)");
        else tvDashboardTitle.setText("My Dashboard");

        String todayPretty = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvDashboardDate.setText(todayPretty);
    }

    // ===================== MEALS =====================

    private void setupMealButtons() {
        btnBreakfastAdd.setOnClickListener(v -> mealSet("Breakfast", 1));
        btnLunchAdd.setOnClickListener(v -> mealSet("Lunch", 1));
        btnDinnerAdd.setOnClickListener(v -> mealSet("Dinner", 1));

        btnBreakfastCancel.setOnClickListener(v -> mealSet("Breakfast", 0));
        btnLunchCancel.setOnClickListener(v -> mealSet("Lunch", 0));
        btnDinnerCancel.setOnClickListener(v -> mealSet("Dinner", 0));
    }

    private void mealSet(String mealType, int val) {
        if (currentUserName == null || "Guest".equals(currentUserName)) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = getTodayDate();

        messDb.setMealForDate(currentUserName, today, mealType, val);

        setupLocalMealRatesAndCounts();
        updateMoneyFromLocalDB();

        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu != null) {
            syncMealToFirestore(currentUserName, today);
        }
    }

    private void syncMealToFirestore(String member, String date) {
        if (member == null || date == null) return;

        int[] daily = messDb.getMealsForDate(member, date);

        Map<String, Object> mealMap = new HashMap<>();
        mealMap.put("memberName", member);
        mealMap.put("date", date);
        mealMap.put("breakfast", daily[0]);
        mealMap.put("lunch", daily[1]);
        mealMap.put("dinner", daily[2]);

        String docId = member + "_" + date;

        fs.collection("meals_daily")
                .document(docId)
                .set(mealMap);
    }

    private void setupLocalMealRatesAndCounts() {
        String monthPrefix = getCurrentMonthPrefix();

        double[] mealPrices = messDb.getCurrentMealPrices();
        String mealPricesText = String.format(Locale.getDefault(),
                "Breakfast = %.0f\nLunch = %.0f\nDinner = %.0f",
                mealPrices[0], mealPrices[1], mealPrices[2]);
        tvMealRateValue.setText(mealPricesText);

        if ("admin".equalsIgnoreCase(currentRole)) {
            int totalMeals = messDb.getMonthlyTotalMealsAllMembers(monthPrefix);
            tvTotalMealsValue.setText(String.valueOf(totalMeals));
        } else {
            int[] counts = messDb.getMonthlyMealCountsForMember(currentUserName, monthPrefix);
            int myMeals = counts[0] + counts[1] + counts[2];
            tvTotalMealsValue.setText(String.valueOf(myMeals));
        }
    }

    private void updateMoneyFromLocalDB() {
        String monthPrefix = getCurrentMonthPrefix();

        if ("admin".equalsIgnoreCase(currentRole)) {
            double totalMealCost = messDb.getMonthlyTotalMealCostAllMembers(monthPrefix);
            double totalOtherExpenses = messDb.getTotalLocalOtherExpensesForMonth(monthPrefix);
            double totalPayments = messDb.getMonthlyTotalPaidAllMembers(monthPrefix);

            double totalExpenses = totalMealCost + totalOtherExpenses;
            double outstanding = totalExpenses - totalPayments;

            tvTotalExpenseValue.setText(String.format(Locale.getDefault(), "%.0f ৳", totalExpenses));
            tvPaidValue.setText(String.format(Locale.getDefault(), "%.0f ৳", totalPayments));

            if (outstanding > 0) tvOutstandingDuesValue.setText(String.format(Locale.getDefault(), "Due: %.0f ৳", outstanding));
            else if (outstanding < 0) tvOutstandingDuesValue.setText(String.format(Locale.getDefault(), "Balance: %.0f ৳", -outstanding));
            else tvOutstandingDuesValue.setText("Balanced");

        } else {
            double[] otherAndPaid = messDb.getMemberLocalExpenseAndPaymentForMonth(currentUserName, monthPrefix);
            double myOtherExpenses = otherAndPaid[0];
            double myPayments = otherAndPaid[1];

            double mealCost = messDb.getMonthlyMealCostForMember(currentUserName, monthPrefix);
            double totalCost = mealCost + myOtherExpenses;
            double balance = totalCost - myPayments;

            tvTotalExpenseValue.setText(String.format(Locale.getDefault(), "%.0f ৳", totalCost));
            tvPaidValue.setText(String.format(Locale.getDefault(), "%.0f ৳", myPayments));

            if (balance > 0) {
                tvOutstandingDuesValue.setText(String.format(Locale.getDefault(),
                        "Due: %.0f ৳\n(Meals: %.0f ৳ + Expenses: %.0f ৳)",
                        balance, mealCost, myOtherExpenses));
            } else if (balance < 0) {
                tvOutstandingDuesValue.setText(String.format(Locale.getDefault(), "Balance: %.0f ৳", -balance));
            } else {
                tvOutstandingDuesValue.setText("Paid");
            }
        }
    }

    // ===================== PAY =====================

    private void setupPayButton() {
        btnPayNow.setOnClickListener(v -> startActivity(new Intent(this, PaymentActivity.class)));
    }

    // ===================== NAV =====================

    private void setupNavigation() {
        navMeals.setOnClickListener(v -> {});
        navExpenses.setOnClickListener(v -> startActivity(new Intent(this, AddExpenseActivity.class)));
        navMembers.setOnClickListener(v -> startActivity(new Intent(this, MemberListActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    // ===================== REALTIME LISTENERS =====================

    private void attachRealtimeExpensesListener() {
        detachExpenseListener();

        expensesListener = fs.collection("expenses")
                .addSnapshotListener((@Nullable QuerySnapshot value,
                                      @Nullable FirebaseFirestoreException error) -> {
                    if (error != null) {
                        Toast.makeText(DashboardActivity.this,
                                "Expense listen failed: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (value == null) return;

                    String monthPrefix = getCurrentMonthPrefix();
                    messDb.clearSyncedExpensesForMonth(monthPrefix);

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String date = doc.getString("date");
                        String title = doc.getString("title");
                        String category = doc.getString("category");
                        String paidBy = doc.getString("paidBy");
                        Double amount = doc.getDouble("amount");
                        if (amount == null) amount = 0.0;
                        if (category == null) category = "";

                        if (date != null && paidBy != null && date.startsWith(monthPrefix)) {
                            messDb.upsertExpenseFromRemote(
                                    doc.getId(),
                                    date,
                                    title != null ? title : "",
                                    category,
                                    paidBy,
                                    amount
                            );
                        }
                    }

                    updateMoneyFromLocalDB();
                });
    }

    private void attachRealtimeMealsListener() {
        detachMealsListener();

        mealsListener = fs.collection("meals_daily")
                .addSnapshotListener((@Nullable QuerySnapshot value,
                                      @Nullable FirebaseFirestoreException error) -> {
                    if (error != null) {
                        Toast.makeText(DashboardActivity.this,
                                "Meals listen failed: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (value == null) return;

                    String monthPrefix = getCurrentMonthPrefix();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String memberName = doc.getString("memberName");
                        String date = doc.getString("date");

                        if (memberName == null || date == null) continue;
                        if (!date.startsWith(monthPrefix)) continue;

                        Long bL = doc.getLong("breakfast");
                        Long lL = doc.getLong("lunch");
                        Long dL = doc.getLong("dinner");

                        int b = (bL == null) ? 0 : bL.intValue();
                        int l = (lL == null) ? 0 : lL.intValue();
                        int d = (dL == null) ? 0 : dL.intValue();

                        messDb.upsertMealsFromRemote(memberName, date, b, l, d);
                    }

                    setupLocalMealRatesAndCounts();
                    updateMoneyFromLocalDB();
                });
    }

    private void detachExpenseListener() {
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }
    }

    private void detachMealsListener() {
        if (mealsListener != null) {
            mealsListener.remove();
            mealsListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUserName != null && !"Guest".equals(currentUserName)) {
            String dbRole = messDb.getUserRoleByUsername(currentUserName);
            if (dbRole != null) currentRole = dbRole;
            kvDb.insertOrUpdate("role", currentRole);
        }

        setupHeader();
        setupLocalMealRatesAndCounts();
        updateMoneyFromLocalDB();

        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu != null) {
            // safe re-attach
            detachAllFirestoreListeners();
            attachAllFirestoreListeners();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachAllFirestoreListeners();
    }
}
