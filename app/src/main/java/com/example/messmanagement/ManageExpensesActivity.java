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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageExpensesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ListView lvExpenses;

    private FirebaseFirestore fs;
    private ListenerRegistration expensesListener;

    private List<String> expenseDisplayList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_expenses);  // 👈 must match file name

        ivBack = findViewById(R.id.ivBack);          // 👈 must match XML id
        lvExpenses = findViewById(R.id.lvExpenses);  // 👈 must match XML id

        ivBack.setOnClickListener(v -> finish());

        fs = FirebaseFirestore.getInstance();
        expenseDisplayList = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                expenseDisplayList
        );
        lvExpenses.setAdapter(adapter);

        attachExpensesListener();
    }

    private void attachExpensesListener() {
        if (expensesListener != null) {
            expensesListener.remove();
        }

        expensesListener = fs.collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(ManageExpensesActivity.this,
                                    "Listen failed: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (value == null) return;

                        expenseDisplayList.clear();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String title = doc.getString("title");
                            String category = doc.getString("category");
                            String paidBy = doc.getString("paidBy");
                            String date = doc.getString("date");
                            Double amount = doc.getDouble("amount");

                            if (title == null) title = "";
                            if (category == null) category = "";
                            if (paidBy == null) paidBy = "";
                            if (date == null) date = "";
                            if (amount == null) amount = 0.0;

                            String line = String.format(Locale.getDefault(),
                                    "%s (%s)\n৳ %.0f | %s | %s",
                                    title,
                                    category,
                                    amount,
                                    paidBy,
                                    date
                            );
                            expenseDisplayList.add(line);
                        }

                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expensesListener != null) {
            expensesListener.remove();
        }
    }
}
