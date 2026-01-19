package com.example.messmanagement;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberDetailsActivity extends AppCompatActivity {

    private ImageView ivBack;

    // ✅ profile fields
    private TextView tvFullName, tvEmail, tvPhone, tvParentPhone, tvAddress, tvRole;

    // existing
    private TextView tvTotalMeals, tvTotalPaid, tvTotalDues, tvMealBreakdown;

    private MessDBHelper messDb;
    private Member member;

    private KeyValueDB kvDb;

    private FirebaseFirestore fs;
    private FirebaseAuth mAuth;
    private ListenerRegistration mealsListener;
    private ListenerRegistration expensesListener;

    private String memberName;   // ✅ email/username
    private String monthPrefix;

    private List<Member> allMembers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_details);

        messDb = new MessDBHelper(this);
        kvDb = new KeyValueDB(this);
        fs = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ivBack = findViewById(R.id.ivBack);

        // ✅ profile views
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvParentPhone = findViewById(R.id.tvParentPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvRole = findViewById(R.id.tvRole);

        // existing
        tvTotalMeals = findViewById(R.id.tvTotalMeals);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvTotalDues = findViewById(R.id.tvTotalDues);
        tvMealBreakdown = findViewById(R.id.tvMealBreakdown);

        ivBack.setOnClickListener(v -> finish());

        monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new java.util.Date());

        memberName = getIntent().getStringExtra("member_name");

        if (memberName == null || memberName.trim().isEmpty()) {
            showMemberPicker();
            return;
        }

        loadMemberAndInit(memberName);
    }

    private void showMemberPicker() {

        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu == null) {
            Toast.makeText(this, "Please login to view members.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fs.collection("users")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(qs -> {

                    allMembers.clear();

                    for (DocumentSnapshot doc : qs.getDocuments()) {

                        String username = doc.getString("username"); // email
                        Boolean isAdminB = doc.getBoolean("isAdmin");

                        // ✅ try multiple keys (depending on your signup save)
                        String fullName = doc.getString("fullName");
                        if (fullName == null) fullName = doc.getString("full_name");
                        if (fullName == null) fullName = doc.getString("name");

                        String contact = doc.getString("contact");
                        if (contact == null) contact = doc.getString("phone");

                        String address = doc.getString("address");

                        String parentContact = doc.getString("parentContact");
                        if (parentContact == null) parentContact = doc.getString("parent_phone");

                        if (username == null || username.trim().isEmpty()) continue;

                        String role = (isAdminB != null && isAdminB) ? "admin" : "member";

                        // keep your list logic same
                        Member m = new Member(-1, username, role, 0, 0.0);
                        allMembers.add(m);

                        // ✅ IMPORTANT: store full profile to local USERS table for offline display
                        messDb.upsertUserFromRemote(
                                doc.getId(),                 // userId (doc id)
                                fullName != null ? fullName : "",
                                username,
                                contact != null ? contact : "",
                                address != null ? address : "",
                                parentContact != null ? parentContact : "",
                                (isAdminB != null && isAdminB),
                                "active"
                        );
                    }

                    if (allMembers.isEmpty()) {
                        Toast.makeText(this, "No active members found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String[] items = new String[allMembers.size()];
                    int preSelect = -1;

                    String myUsername = kvDb.getValueByKey("username");

                    for (int i = 0; i < allMembers.size(); i++) {
                        Member m = allMembers.get(i);

                        // ✅ show full name in picker (fallback to email)
                        String fullName = messDb.getFullNameByUsername(m.name);
                        if (fullName == null || fullName.trim().isEmpty()) fullName = m.name;

                        String r = (m.role == null) ? "member" : m.role;
                        items[i] = fullName + "  (" + r + ")\n" + m.name;

                        if (myUsername != null && myUsername.equalsIgnoreCase(m.name)) {
                            preSelect = i;
                        }
                    }

                    AlertDialog.Builder b = new AlertDialog.Builder(this);
                    b.setTitle("Select a member");

                    final int finalPreSelect = preSelect;
                    b.setSingleChoiceItems(items, preSelect, (dialog, which) -> {});

                    b.setPositiveButton("Open", (dialog, which) -> {
                        AlertDialog ad = (AlertDialog) dialog;
                        int checked = ad.getListView().getCheckedItemPosition();
                        if (checked < 0) checked = (finalPreSelect >= 0) ? finalPreSelect : 0;

                        Member chosen = allMembers.get(checked);
                        memberName = chosen.name; // email
                        loadMemberAndInit(memberName);
                    });

                    b.setNegativeButton("Cancel", (dialog, which) -> finish());
                    b.show();
                })
                .addOnFailureListener(e -> {
                    // fallback local only
                    allMembers = messDb.getAllMembers();
                    if (allMembers == null || allMembers.isEmpty()) {
                        Toast.makeText(this, "No members found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String[] items = new String[allMembers.size()];
                    int preSelect = -1;
                    String myUsername = kvDb.getValueByKey("username");

                    for (int i = 0; i < allMembers.size(); i++) {
                        Member m = allMembers.get(i);

                        String fullName = messDb.getFullNameByUsername(m.name);
                        if (fullName == null || fullName.trim().isEmpty()) fullName = m.name;

                        String role = (m.role == null) ? "member" : m.role;
                        items[i] = fullName + "  (" + role + ")\n" + m.name;

                        if (myUsername != null && myUsername.equalsIgnoreCase(m.name)) {
                            preSelect = i;
                        }
                    }

                    AlertDialog.Builder b = new AlertDialog.Builder(this);
                    b.setTitle("Select a member");

                    final int finalPreSelect = preSelect;
                    b.setSingleChoiceItems(items, preSelect, (dialog, which) -> {});

                    b.setPositiveButton("Open", (dialog, which) -> {
                        AlertDialog ad = (AlertDialog) dialog;
                        int checked = ad.getListView().getCheckedItemPosition();
                        if (checked < 0) checked = (finalPreSelect >= 0) ? finalPreSelect : 0;

                        Member chosen = allMembers.get(checked);
                        memberName = chosen.name;
                        loadMemberAndInit(memberName);
                    });

                    b.setNegativeButton("Cancel", (dialog, which) -> finish());
                    b.show();
                });
    }

    private void loadMemberAndInit(String targetMemberName) {
        this.memberName = targetMemberName;

        member = messDb.getMemberByName(memberName);
        if (member == null) {
            member = new Member(-1, memberName, "member", 0, 0.0);
        }

        // ✅ show cached immediately
        updateUiFromLocal();

        detachListeners();

        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu != null) {

            // ✅ pull profile once from Firestore and cache locally (so everyone sees full info)
            fs.collection("users")
                    .whereEqualTo("username", memberName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(qs -> {
                        if (qs == null || qs.isEmpty()) return;
                        DocumentSnapshot doc = qs.getDocuments().get(0);

                        Boolean isAdminB = doc.getBoolean("isAdmin");

                        String fullName = doc.getString("fullName");
                        if (fullName == null) fullName = doc.getString("full_name");
                        if (fullName == null) fullName = doc.getString("name");

                        String contact = doc.getString("contact");
                        if (contact == null) contact = doc.getString("phone");

                        String address = doc.getString("address");

                        String parentContact = doc.getString("parentContact");
                        if (parentContact == null) parentContact = doc.getString("parent_phone");

                        String status = doc.getString("status");
                        if (status == null) status = "active";

                        messDb.upsertUserFromRemote(
                                doc.getId(),
                                fullName != null ? fullName : "",
                                memberName,
                                contact != null ? contact : "",
                                address != null ? address : "",
                                parentContact != null ? parentContact : "",
                                (isAdminB != null && isAdminB),
                                status
                        );

                        // ✅ refresh UI after caching profile
                        updateUiFromLocal();
                    });

            attachMealsListener();
            attachExpensesListener_ALL();
        } else {
            Toast.makeText(this,
                    "Offline mode: showing cached data only.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateUiFromLocal() {
        // ✅ profile (from local USERS table)
        String fullName = messDb.getFullNameByUsername(member.name);
        String phone = messDb.getContactByUsername(member.name);
        String parentPhone = messDb.getParentContactByUsername(member.name);
        String address = messDb.getAddressByUsername(member.name);

        String role = messDb.getUserRoleByUsername(member.name);
        if (role == null) role = (member.role != null ? member.role : "member");

        tvFullName.setText("Full Name: " + (fullName == null || fullName.trim().isEmpty() ? "-" : fullName));
        tvEmail.setText("Email: " + member.name);
        tvRole.setText("Role: " + role);
        tvPhone.setText("Phone: " + (phone == null || phone.trim().isEmpty() ? "-" : phone));
        tvParentPhone.setText("Parent Phone: " + (parentPhone == null || parentPhone.trim().isEmpty() ? "-" : parentPhone));
        tvAddress.setText("Address: " + (address == null || address.trim().isEmpty() ? "-" : address));

        // ✅ existing money + meal breakdown
        ExpenseBreakdown breakdown =
                messDb.getMemberExpenseBreakdown(member.name, monthPrefix);

        double[] mealPrices = messDb.getCurrentMealPrices();
        double breakfastPrice = mealPrices[0];
        double lunchPrice = mealPrices[1];
        double dinnerPrice = mealPrices[2];

        int breakfast = breakdown.breakfastCount;
        int lunch = breakdown.lunchCount;
        int dinner = breakdown.dinnerCount;

        double breakfastCost = breakfast * breakfastPrice;
        double lunchCost = lunch * lunchPrice;
        double dinnerCost = dinner * dinnerPrice;

        int totalMeals = breakfast + lunch + dinner;
        tvTotalMeals.setText("Total Meals: " + totalMeals);

        tvMealBreakdown.setText(String.format(Locale.getDefault(),
                "Breakfast: %d × %.0f = %.0f ৳\n" +
                        "Lunch: %d × %.0f = %.0f ৳\n" +
                        "Dinner: %d × %.0f = %.0f ৳\n\n" +
                        "Meal Cost Total: %.0f ৳\n" +
                        "Other Expenses: %.0f ৳",
                breakfast, breakfastPrice, breakfastCost,
                lunch, lunchPrice, lunchCost,
                dinner, dinnerPrice, dinnerCost,
                breakdown.mealCost,
                breakdown.otherExpensesShare
        ));

        tvTotalPaid.setText(String.format(Locale.getDefault(),
                "Total Paid: %.0f ৳", breakdown.paidAmount));

        double balance = breakdown.balance;
        if (balance > 0) {
            tvTotalDues.setText(String.format(Locale.getDefault(),
                    "Due: %.0f ৳", balance));
        } else if (balance < 0) {
            tvTotalDues.setText(String.format(Locale.getDefault(),
                    "Advance: %.0f ৳", -balance));
        } else {
            tvTotalDues.setText("Balanced");
        }
    }

    private void attachMealsListener() {
        mealsListener = fs.collection("meals_daily")
                .whereEqualTo("memberName", memberName)
                .addSnapshotListener((@Nullable QuerySnapshot value,
                                      @Nullable FirebaseFirestoreException error) -> {
                    if (error != null || value == null) {
                        updateUiFromLocal();
                        return;
                    }

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String date = doc.getString("date");
                        if (date == null || !date.startsWith(monthPrefix)) continue;

                        Long bL = doc.getLong("breakfast");
                        Long lL = doc.getLong("lunch");
                        Long dL = doc.getLong("dinner");

                        int b = (bL == null) ? 0 : bL.intValue();
                        int l = (lL == null) ? 0 : lL.intValue();
                        int d = (dL == null) ? 0 : dL.intValue();

                        messDb.upsertMealsFromRemote(memberName, date, b, l, d);
                    }

                    updateUiFromLocal();
                });
    }

    private void attachExpensesListener_ALL() {
        expensesListener = fs.collection("expenses")
                .addSnapshotListener((@Nullable QuerySnapshot value,
                                      @Nullable FirebaseFirestoreException error) -> {
                    if (error != null || value == null) {
                        updateUiFromLocal();
                        return;
                    }

                    messDb.clearSyncedExpensesForMonth(monthPrefix);

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String date = doc.getString("date");
                        if (date == null || !date.startsWith(monthPrefix)) continue;

                        String title = doc.getString("title");
                        String category = doc.getString("category");
                        String paidBy = doc.getString("paidBy");
                        Double amount = doc.getDouble("amount");
                        if (amount == null) amount = 0.0;

                        if (paidBy == null) continue;

                        messDb.upsertExpenseFromRemote(
                                doc.getId(),
                                date,
                                title != null ? title : "",
                                category != null ? category : "",
                                paidBy,
                                amount
                        );
                    }

                    updateUiFromLocal();
                });
    }

    private void detachListeners() {
        if (mealsListener != null) {
            mealsListener.remove();
            mealsListener = null;
        }
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachListeners();
    }
}
