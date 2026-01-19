package com.example.messmanagement;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 1001;

    private KeyValueDB kvDb;
    private MessDBHelper messDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        kvDb = new KeyValueDB(this);
        messDb = new MessDBHelper(this);

        // ✅ IMPORTANT: first-install / restore-safe session reset
        handleFirstRunReset();

        askNotificationPermission();
        new Handler(Looper.getMainLooper()).postDelayed(this::goNext, 800);



    }

    private void handleFirstRunReset() {
        SharedPreferences sp = getSharedPreferences("app_boot", MODE_PRIVATE);
        boolean firstRunDone = sp.getBoolean("first_run_done", false);

        if (!firstRunDone) {
            // Clear only login/session keys (not your whole DB)
            kvDb.insertOrUpdate("is_logged_in", "false");
            kvDb.insertOrUpdate("username", "");
            kvDb.insertOrUpdate("role", "");

            // If you also store session in UserPrefs, clear that too
            SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userPrefs.edit()
                    .remove("is_logged_in")
                    .remove("is_admin")
                    .remove("user_id")
                    .remove("user_name")
                    .apply();

            sp.edit().putBoolean("first_run_done", true).apply();
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFICATIONS
                );
            }
        }
    }

    private void goNext() {
        String isLoggedIn = kvDb.getValueByKey("is_logged_in"); // "true" or null
        String username = kvDb.getValueByKey("username");

        boolean sessionOk =
                "true".equalsIgnoreCase(isLoggedIn)
                        && username != null
                        && !username.trim().isEmpty();

        if (!sessionOk) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // ✅ Safety: status must be ACTIVE, otherwise force logout
        String status = messDb.getUserStatusByUsername(username);
        if (status == null) status = "active";

        if ("pending".equalsIgnoreCase(status) || "deleted".equalsIgnoreCase(status)) {
            kvDb.insertOrUpdate("is_logged_in", "false");
            kvDb.insertOrUpdate("username", "");
            kvDb.insertOrUpdate("role", "");

            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // no change needed
    }
}
