package com.example.messmanagement;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminLiveExpensesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ListView lvExpenses;

    private ArrayList<String> expensesData = new ArrayList<>();
    private ArrayAdapter<String> expensesAdapter;

    private FirebaseFirestore fs;
    private ListenerRegistration expensesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_live_expenses);

        fs = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);
        lvExpenses = findViewById(R.id.lvExpenses);

        findViewById(R.id.ivBack).setOnClickListener(v -> onBackPressed());


        ivBack.setOnClickListener(v -> finish());

        expensesAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                expensesData
        );
        lvExpenses.setAdapter(expensesAdapter);

        attachExpensesListener();
    }

    private String getCurrentMonthPrefix() {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
    }

    private void attachExpensesListener() {
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }

        final String monthPrefix = getCurrentMonthPrefix();

        expensesListener = fs.collection("expenses")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(AdminLiveExpensesActivity.this,
                                    "Expenses listen failed: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (value == null) return;

                        expensesData.clear();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String date = doc.getString("date");
                            if (date == null || !date.startsWith(monthPrefix)) continue;

                            String title = doc.getString("title");
                            String category = doc.getString("category");
                            String paidBy = doc.getString("paidBy");
                            Double amount = doc.getDouble("amount");

                            if (title == null) title = "(No title)";
                            if (category == null) category = "";
                            if (paidBy == null) paidBy = "";
                            if (amount == null) amount = 0.0;

                            String row = date + " | " + title +
                                    " (" + category + ") by " + paidBy +
                                    " = " + String.format(Locale.getDefault(), "%.0f ৳", amount);

                            expensesData.add(row);
                        }

                        if (expensesData.isEmpty()) {
                            expensesData.add("No expenses found for this month");
                        }

                        expensesAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }
    }
}
