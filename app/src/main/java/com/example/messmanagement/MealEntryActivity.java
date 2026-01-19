package com.example.messmanagement;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MealEntryActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvBreakfastAmount, tvLunchAmount, tvDinnerAmount;
    private Button btnBreakfastEntry, btnLunchEntry, btnDinnerEntry, btnCart;

    private MessDBHelper messDb;
    private KeyValueDB kvDb;
    private String currentUserName;
    private String currentRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_entry);

        messDb = new MessDBHelper(this);
        kvDb = new KeyValueDB(this);

        currentUserName = kvDb.getValueByKey("username");
        currentRole = kvDb.getValueByKey("role");
        if (currentUserName == null) currentUserName = "Guest";
        if (currentRole == null) currentRole = "member";

        ivBack = findViewById(R.id.ivBack);
        tvBreakfastAmount = findViewById(R.id.tvBreakfastAmount);
        tvLunchAmount = findViewById(R.id.tvLunchAmount);
        tvDinnerAmount = findViewById(R.id.tvDinnerAmount);
        btnBreakfastEntry = findViewById(R.id.btnBreakfastEntry);
        btnLunchEntry = findViewById(R.id.btnLunchEntry);
        btnDinnerEntry = findViewById(R.id.btnDinnerEntry);
        btnCart = findViewById(R.id.btnCart);

        ivBack.setOnClickListener(v -> finish());

        // Show fixed prices
        tvBreakfastAmount.setText("50 Taka");
        tvLunchAmount.setText("150 Taka");
        tvDinnerAmount.setText("150 Taka");

        // When user taps, it means: YES for today's meal
        btnBreakfastEntry.setOnClickListener(v -> addMeal("Breakfast"));
        btnLunchEntry.setOnClickListener(v -> addMeal("Lunch"));
        btnDinnerEntry.setOnClickListener(v -> addMeal("Dinner"));

        btnCart.setOnClickListener(v ->
                Toast.makeText(this, "Cart feature not implemented", Toast.LENGTH_SHORT).show());
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void addMeal(String mealType) {
        String today = getTodayDate();

        // 1 = YES for today's meal (0 = NO)
        messDb.setMealForDate(currentUserName, today, mealType, 1);

        Toast.makeText(
                this,
                "Saved " + mealType + " = YES for " + currentUserName + " (" + today + ")",
                Toast.LENGTH_SHORT
        ).show();
    }
}
