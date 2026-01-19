package com.example.messmanagement;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etAmount, etNote;
    private Button btnSave;

    private MessDBHelper messDb;
    private KeyValueDB kvDb;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        messDb = new MessDBHelper(this);
        kvDb = new KeyValueDB(this);

        currentUserName = kvDb.getValueByKey("username");
        if (currentUserName == null) currentUserName = "Guest";

        ivBack = findViewById(R.id.ivBack);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        btnSave = findViewById(R.id.btnSave);

        ivBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> savePaymentOfflineFirst());
    }

    private String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void savePaymentOfflineFirst() {
        if ("Guest".equals(currentUserName)) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etNote.getText().toString().trim();
        String date = getToday();

        // ✅ stable id prevents cloud duplicates
        String remoteId = UUID.randomUUID().toString();

        // shown in expenses list
        String title = note.isEmpty() ? "Payment" : note;

        // ✅ Save locally as PENDING (category PAYMENT)
        messDb.addExpensePending(
                remoteId,
                title,
                amount,
                MessDBHelper.CATEGORY_PAYMENT,
                currentUserName,
                date
        );

        // ✅ trigger sync (WorkManager runs when internet available)
        SyncScheduler.runOneTimeSyncNow(this);

        Toast.makeText(this, "Payment successful.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
