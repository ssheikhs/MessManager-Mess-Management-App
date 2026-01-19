package com.example.messmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

public class MemberExpensesActivity extends AppCompatActivity {

    private TextView tvTitle, tvSummary, tvEmptyExpenses;
    private ListView lvExpenses;
    private MessDBHelper messDb;
    private String memberName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_expenses);

        messDb = new MessDBHelper(this);

        memberName = getIntent().getStringExtra("member_name");
        if (memberName == null) memberName = "Unknown";

        tvTitle = findViewById(R.id.tvTitle);
        tvSummary = findViewById(R.id.tvSummary);
        tvEmptyExpenses = findViewById(R.id.tvEmptyExpenses);
        lvExpenses = findViewById(R.id.lvExpenses);

        ((ImageView) findViewById(R.id.ivBack)).setOnClickListener(v -> finish());

        tvTitle.setText("Expenses of " + memberName);

        loadData();
    }

    private void loadData() {
        List<Expense> expenses = messDb.getExpensesByMember(memberName);

        if (expenses.isEmpty()) {
            tvEmptyExpenses.setVisibility(View.VISIBLE);
            lvExpenses.setVisibility(View.GONE);
            tvSummary.setText("Total Spent: 0 ৳");
            return;
        }

        tvEmptyExpenses.setVisibility(View.GONE);
        lvExpenses.setVisibility(View.VISIBLE);

        double total = 0;
        for (Expense e : expenses) total += e.amount;

        tvSummary.setText(String.format(Locale.getDefault(), "Total Spent: %.0f ৳", total));

        lvExpenses.setAdapter(new MemberExpensesAdapter(this, expenses));
    }
}
