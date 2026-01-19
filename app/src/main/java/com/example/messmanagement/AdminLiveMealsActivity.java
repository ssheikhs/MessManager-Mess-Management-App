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

public class AdminLiveMealsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ListView lvMeals;

    private final ArrayList<String> mealsData = new ArrayList<>();
    private ArrayAdapter<String> mealsAdapter;

    private FirebaseFirestore fs;
    private ListenerRegistration mealsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_live_meals);

        fs = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);
        lvMeals = findViewById(R.id.lvMeals);

        ivBack.setOnClickListener(v -> finish());

        mealsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                mealsData
        );
        lvMeals.setAdapter(mealsAdapter);

        attachMealsListener();
    }

    private String getCurrentMonthPrefix() {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
    }

    private void attachMealsListener() {
        if (mealsListener != null) {
            mealsListener.remove();
            mealsListener = null;
        }

        final String monthPrefix = getCurrentMonthPrefix();

        mealsListener = fs.collection("meals_daily")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(AdminLiveMealsActivity.this,
                                    "Meals listen failed: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (value == null) return;

                        mealsData.clear();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String member = doc.getString("memberName");
                            String date = doc.getString("date");
                            Long bL = doc.getLong("breakfast");
                            Long lL = doc.getLong("lunch");
                            Long dL = doc.getLong("dinner");

                            if (member == null || date == null) continue;
                            if (!date.startsWith(monthPrefix)) continue;

                            int b = (bL == null) ? 0 : bL.intValue();
                            int l = (lL == null) ? 0 : lL.intValue();
                            int d = (dL == null) ? 0 : dL.intValue();

                            String bTxt = (b == 1) ? "YES" : "NO";
                            String lTxt = (l == 1) ? "YES" : "NO";
                            String dTxt = (d == 1) ? "YES" : "NO";

                            String row =
                                    member + "  (" + date + ")\n" +
                                            "Breakfast: " + bTxt + "   Lunch: " + lTxt + "   Dinner: " + dTxt;

                            mealsData.add(row);
                        }

                        if (mealsData.isEmpty()) {
                            mealsData.add("No meals found for this month");
                        }

                        mealsAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mealsListener != null) {
            mealsListener.remove();
            mealsListener = null;
        }
    }
}
