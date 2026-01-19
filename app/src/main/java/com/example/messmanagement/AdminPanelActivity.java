package com.example.messmanagement;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminPanelActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Button btnAddMember, btnRemoveMember, btnApproveMembers,
            btnSetMealPrice, btnShowMonthlyReport,
            btnLiveMeals, btnLiveExpenses;

    private MessDBHelper messDb;
    private String currentAdminUid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        messDb = new MessDBHelper(this);

        // ✅ Prefer Firebase UID (works on any device)
        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        if (fu != null) {
            currentAdminUid = fu.getUid();
        } else {
            // fallback (older flow)
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            currentAdminUid = prefs.getString("user_id", "");
        }

        // ----- View binding -----
        ivBack = findViewById(R.id.ivBack);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnRemoveMember = findViewById(R.id.btnRemoveMember);
        btnApproveMembers = findViewById(R.id.btnApproveMembers);
        btnSetMealPrice = findViewById(R.id.btnSetMealPrice);
        btnShowMonthlyReport = findViewById(R.id.btnShowMonthlyReport);
        btnLiveMeals = findViewById(R.id.btnLiveMeals);
        btnLiveExpenses = findViewById(R.id.btnLiveExpenses);

        ivBack.setOnClickListener(v -> finish());

        btnAddMember.setOnClickListener(v -> {
            Intent i = new Intent(this, SignupActivity.class);
            i.putExtra("created_by_admin", true);
            startActivity(i);
        });

        btnRemoveMember.setOnClickListener(v -> {
            Intent i = new Intent(this, MemberListActivity.class);
            i.putExtra("mode", "remove");
            i.putExtra("admin_id", currentAdminUid); // prevent self-removal
            startActivity(i);
        });

        btnApproveMembers.setOnClickListener(v -> {
            Intent i = new Intent(this, ApproveMembersActivity.class);
            startActivity(i);
        });

        btnSetMealPrice.setOnClickListener(v ->
                startActivity(new Intent(this, SetMealPricesActivity.class)));

        btnShowMonthlyReport.setOnClickListener(v ->
                startActivity(new Intent(this, MonthlySummaryActivity.class)));

        btnLiveMeals.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLiveMealsActivity.class)));

        btnLiveExpenses.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLiveExpensesActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ Safety check: if user is not admin locally, block
        if (currentAdminUid == null || currentAdminUid.trim().isEmpty()) {
            Toast.makeText(this, "Admin session missing. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!messDb.isUserAdmin(currentAdminUid)) {
            Toast.makeText(this, "You are no longer an admin", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
