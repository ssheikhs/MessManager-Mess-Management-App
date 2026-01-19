package com.example.messmanagement;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AddExpenseActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etExpenseTitle, etAmount, etDate;
    private Spinner spCategory, spPaidBy;

    private MessDBHelper messDb;
    private KeyValueDB kvDb;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        messDb = new MessDBHelper(this);
        kvDb = new KeyValueDB(this);

        currentUserName = kvDb.getValueByKey("username");
        if (currentUserName == null) currentUserName = "Guest";

        ivBack = findViewById(R.id.ivBack);
        etExpenseTitle = findViewById(R.id.etExpenseTitle);
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etDate);
        spCategory = findViewById(R.id.spCategory);
        spPaidBy = findViewById(R.id.spPaidBy);

        ivBack.setOnClickListener(v -> finish());

        setupSpinners();
        setupDatePicker();

        findViewById(R.id.btnEntry).setOnClickListener(v -> saveExpenseOfflineFirst());
    }

    private void setupSpinners() {
        // Keep PAYMENT out of normal expenses (PaymentActivity handles it)
        String[] categories = {"Food", "Market", "Utility", "Others"};

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        List<Member> members = messDb.getAllMembers();
        String[] memberNames;

        if (members == null || members.isEmpty()) {
            memberNames = new String[]{"Unknown"};
        } else {
            memberNames = new String[members.size()];
            for (int i = 0; i < members.size(); i++) {
                memberNames[i] = members.get(i).name;
            }
        }

        ArrayAdapter<String> memberAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, memberNames
        );
        memberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPaidBy.setAdapter(memberAdapter);

        // Preselect current user if present in members list
        if (currentUserName != null && !currentUserName.equals("Guest")) {
            for (int i = 0; i < memberNames.length; i++) {
                if (memberNames[i].equals(currentUserName)) {
                    spPaidBy.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dpd = new DatePickerDialog(
                    this,
                    (DatePicker view, int year1, int month1, int dayOfMonth) -> {
                        String monthStr = String.format(Locale.getDefault(), "%02d", (month1 + 1));
                        String dayStr = String.format(Locale.getDefault(), "%02d", dayOfMonth);
                        etDate.setText(year1 + "-" + monthStr + "-" + dayStr);
                    },
                    year, month, day
            );
            dpd.show();
        });

        Calendar calendar = Calendar.getInstance();
        String today = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
        etDate.setText(today);
    }

    private void saveExpenseOfflineFirst() {
        if ("Guest".equals(currentUserName)) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etExpenseTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (title.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spCategory.getSelectedItem().toString();
        String paidBy = spPaidBy.getSelectedItem().toString();

        // stable remote id -> prevents cloud duplicates
        String remoteId = UUID.randomUUID().toString();

        // save locally as PENDING (sync_state=0)
        messDb.addExpensePending(remoteId, title, amount, category, paidBy, date);

        // trigger background sync when internet returns
        SyncScheduler.runOneTimeSyncNow(this);

        Toast.makeText(this, "Expenses added successfully.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
