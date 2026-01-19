package com.example.messmanagement;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SetMealPricesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etBreakfastPrice, etLunchPrice, etDinnerPrice;
    private Button btnSavePrices;

    private MessDBHelper messDb;
    private FirebaseFirestore fs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_meal_prices);

        messDb = new MessDBHelper(this);
        fs = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);
        etBreakfastPrice = findViewById(R.id.etBreakfastPrice);
        etLunchPrice = findViewById(R.id.etLunchPrice);
        etDinnerPrice = findViewById(R.id.etDinnerPrice);
        btnSavePrices = findViewById(R.id.btnSavePrices);

        loadCurrentPrices();

        ivBack.setOnClickListener(v -> finish());
        btnSavePrices.setOnClickListener(v -> saveMealPrices());
    }

    private void loadCurrentPrices() {
        double[] prices = messDb.getCurrentMealPrices();
        etBreakfastPrice.setText(String.valueOf((int) prices[0]));
        etLunchPrice.setText(String.valueOf((int) prices[1]));
        etDinnerPrice.setText(String.valueOf((int) prices[2]));
    }

    private void saveMealPrices() {
        String breakfastStr = etBreakfastPrice.getText().toString().trim();
        String lunchStr = etLunchPrice.getText().toString().trim();
        String dinnerStr = etDinnerPrice.getText().toString().trim();

        if (breakfastStr.isEmpty() || lunchStr.isEmpty() || dinnerStr.isEmpty()) {
            Toast.makeText(this, "Please enter all meal prices", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double breakfastPrice = Double.parseDouble(breakfastStr);
            double lunchPrice = Double.parseDouble(lunchStr);
            double dinnerPrice = Double.parseDouble(dinnerStr);

            if (breakfastPrice < 0 || lunchPrice < 0 || dinnerPrice < 0) {
                Toast.makeText(this, "Prices cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

// ✅ FIX: match MessDBHelper signature (double,double,double,String)
            boolean localOk = messDb.insertMealPriceHistory(breakfastPrice, lunchPrice, dinnerPrice, today);
            if (!localOk) {
                Toast.makeText(this, "Failed to update meal prices (local)", Toast.LENGTH_SHORT).show();
                return;
            }


            // ✅ Save to Firestore so other devices sync instantly
            Map<String, Object> data = new HashMap<>();
            data.put("effectiveDate", today);
            data.put("breakfastPrice", breakfastPrice);
            data.put("lunchPrice", lunchPrice);
            data.put("dinnerPrice", dinnerPrice);
            data.put("createdAt", FieldValue.serverTimestamp());

            fs.collection("meal_prices").document(today)
                    .set(data)
                    .addOnSuccessListener(r -> {
                        Toast.makeText(this, "Meal prices updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Saved locally, but sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}
