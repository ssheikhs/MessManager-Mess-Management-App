package com.example.messmanagement;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private RadioGroup rgRole;
    private RadioButton rbMember, rbAdmin;
    private Button btnLogin;
    private TextView tvLoginCreateAccount, tvCreateAccountBottom;

    private CheckBox cbRememberUser, cbRememberPassword;

    private KeyValueDB kvDb;
    private MessDBHelper messDb;

    private FirebaseAuth mAuth;
    private FirebaseFirestore fs;

    private static final String KV_REMEMBER_USER = "remember_user";
    private static final String KV_REMEMBER_PASS = "remember_pass";
    private static final String KV_SAVED_USER    = "saved_username";
    private static final String KV_SAVED_PASS    = "saved_password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        kvDb = new KeyValueDB(this);
        messDb = new MessDBHelper(this);
        mAuth = FirebaseAuth.getInstance();
        fs = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);

        // These may exist in your other layout version; keeping safe

        btnLogin = findViewById(R.id.btnLogin);

        cbRememberUser = findViewById(R.id.cbRememberUser);
        cbRememberPassword = findViewById(R.id.cbRememberPassword);

        tvLoginCreateAccount = findViewById(R.id.tvLoginCreateAccount);
        tvCreateAccountBottom = findViewById(R.id.tvCreateAccountBottom);

        restoreRememberedLogin();

        btnLogin.setOnClickListener(v -> doLogin());

        tvLoginCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
        tvCreateAccountBottom.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
    }

    private void restoreRememberedLogin() {
        String rememberUser = kvDb.getValueByKey(KV_REMEMBER_USER);
        String rememberPass = kvDb.getValueByKey(KV_REMEMBER_PASS);

        boolean ru = "true".equalsIgnoreCase(rememberUser);
        boolean rp = "true".equalsIgnoreCase(rememberPass);

        cbRememberUser.setChecked(ru);
        cbRememberPassword.setChecked(rp);

        if (ru) {
            String savedUser = kvDb.getValueByKey(KV_SAVED_USER);
            if (savedUser != null && !savedUser.trim().isEmpty()) {
                etUsername.setText(savedUser);
            }
        }

        if (rp) {
            String savedPass = kvDb.getValueByKey(KV_SAVED_PASS);
            if (savedPass != null && !savedPass.trim().isEmpty()) {
                etPassword.setText(savedPass);
            }
        }
    }

    private void saveRememberedLogin(String emailJustLoggedIn) {
        boolean ru = cbRememberUser.isChecked();
        boolean rp = cbRememberPassword.isChecked();

        kvDb.insertOrUpdate(KV_REMEMBER_USER, ru ? "true" : "false");
        kvDb.insertOrUpdate(KV_REMEMBER_PASS, rp ? "true" : "false");

        if (ru) {
            kvDb.insertOrUpdate(KV_SAVED_USER, emailJustLoggedIn);
        } else {
            kvDb.insertOrUpdate(KV_SAVED_USER, "");
        }

        if (rp) {
            kvDb.insertOrUpdate(KV_SAVED_PASS, etPassword.getText().toString());
        } else {
            kvDb.insertOrUpdate(KV_SAVED_PASS, "");
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null &&
                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim(); // email
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
            return;
        }

        // OFFLINE: SQLite only (device must have cached user once)
        if (!isOnline()) {
            loginOfflineSQLite(username, password);
            return;
        }

        // ONLINE: FirebaseAuth
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnSuccessListener(r -> {
                    FirebaseUser fu = mAuth.getCurrentUser();
                    if (fu == null) {
                        Toast.makeText(this, "Login failed, try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String email = fu.getEmail();
                    if (email == null) email = username;

                    fetchUserFromFirestoreAndCacheThenLogin(fu.getUid(), email, password);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void fetchUserFromFirestoreAndCacheThenLogin(String uid, String email, String password) {
        fs.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        String contact = doc.getString("contact");
                        String address = doc.getString("address");
                        String parentContact = doc.getString("parentContact");

                        Boolean isAdminBool = doc.getBoolean("isAdmin");
                        boolean isAdmin = isAdminBool != null && isAdminBool;

                        String status = doc.getString("status");
                        if (status == null) status = "active";

                        // Firestore -> SQLite cache
                        messDb.upsertUserFromRemote(uid, fullName, email, contact, address, parentContact, isAdmin, status);

                        // Save password locally for offline login on THIS device
                        messDb.updateLocalPasswordByUsername(email, password);
                    }

                    handlePostLogin(email);
                })
                .addOnFailureListener(e -> handlePostLogin(email));
    }

    private void handlePostLogin(String email) {
        String status = messDb.getUserStatusByUsername(email);
        if (status == null) status = "active";

        if ("deleted".equalsIgnoreCase(status)) {
            kvDb.insertOrUpdate("is_logged_in", "false");
            mAuth.signOut();
            Toast.makeText(this, "Your account has been removed. Contact admin.", Toast.LENGTH_LONG).show();
            return;
        }

        if ("pending".equalsIgnoreCase(status)) {
            kvDb.insertOrUpdate("is_logged_in", "false");
            Toast.makeText(this, "Your account is pending approval.", Toast.LENGTH_LONG).show();
            return;
        }

        String role = messDb.getUserRoleByUsername(email);
        if (role == null) role = "member";

        kvDb.insertOrUpdate("username", email);
        kvDb.insertOrUpdate("role", role);
        kvDb.insertOrUpdate("is_logged_in", "true");

        // ✅ Save remember options here
        saveRememberedLogin(email);

        Toast.makeText(this, "Login successful as " + role, Toast.LENGTH_SHORT).show();
        navigateAfterLogin();
    }

    private void loginOfflineSQLite(@NonNull String username, @NonNull String password) {
        boolean ok = messDb.checkLocalLoginByUsernamePassword(username, password);
        if (!ok) {
            Toast.makeText(this,
                    "Offline login needs this device to have logged in once online.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String status = messDb.getUserStatusByUsername(username);
        if ("deleted".equalsIgnoreCase(status)) {
            Toast.makeText(this, "Account removed. Contact admin.", Toast.LENGTH_LONG).show();
            return;
        }
        if ("pending".equalsIgnoreCase(status)) {
            Toast.makeText(this, "Account pending approval.", Toast.LENGTH_LONG).show();
            return;
        }

        String role = messDb.getUserRoleByUsername(username);
        if (role == null) role = "member";

        kvDb.insertOrUpdate("username", username);
        kvDb.insertOrUpdate("role", role);

        // ✅ Save remember options for offline login too
        saveRememberedLogin(username);

        Toast.makeText(this, "Offline login as " + role, Toast.LENGTH_SHORT).show();
        navigateAfterLogin();
    }

    private void navigateAfterLogin() {
        Intent i = new Intent(this, DashboardActivity.class);
        startActivity(i);
        finish();
    }
}
