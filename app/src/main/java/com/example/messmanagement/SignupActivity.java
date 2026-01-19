package com.example.messmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText etFullName, etContact, etAddress, etParentContact,
            etUsername, etPassword, etConfirmPassword;
    private RadioGroup rgRole;
    private RadioButton rbMember, rbAdmin;
    private Button btnSignUp;
    private ImageView ivBack;

    private MessDBHelper messDb;
    private boolean createdByAdmin = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore fs;

    private KeyValueDB kvDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        messDb = new MessDBHelper(this);
        mAuth = FirebaseAuth.getInstance();
        fs = FirebaseFirestore.getInstance();
        kvDb = new KeyValueDB(this);

        createdByAdmin = getIntent().getBooleanExtra("created_by_admin", false);

        initializeViews();
        setupClickListeners();

        if (createdByAdmin) {
            rbMember.setChecked(true);
            rbAdmin.setVisibility(View.GONE);
        }
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        etFullName = findViewById(R.id.etFullName);
        etContact = findViewById(R.id.etContact);
        etAddress = findViewById(R.id.etAddress);
        etParentContact = findViewById(R.id.etParentContact);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        rgRole = findViewById(R.id.rgRole);
        rbMember = findViewById(R.id.rbMember);
        rbAdmin = findViewById(R.id.rbAdmin);
        btnSignUp = findViewById(R.id.btnSignUp);

        ivBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnSignUp.setOnClickListener(v -> {
            if (validateInputs()) registerUser();
        });
    }

    private boolean validateInputs() {
        String fullName = etFullName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String parentContact = etParentContact.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String username = etUsername.getText().toString().trim(); // email
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty()) { etFullName.setError("Full name is required"); return false; }
        if (fullName.length() > 20) { etFullName.setError("Name must be at most 20 characters"); return false; }

        // Phone: allow + then digits, total length 11-14 digits (not counting +)
        String phoneRegex = "^\\+?\\d{11,14}$";

        if (contact.isEmpty()) { etContact.setError("Contact number is required"); return false; }
        if (!contact.matches(phoneRegex)) {
            etContact.setError("Contact must be 11 to 14 digits (you can use +88)");
            return false;
        }

        if (!parentContact.isEmpty()) {
            if (!parentContact.matches(phoneRegex)) {
                etParentContact.setError("Parent contact must be 11 to 14 digits (you can use +88)");
                return false;
            }
        }

        if (address.length() > 50) { etAddress.setError("Address must be at most 50 characters"); return false; }

        if (username.isEmpty()) { etUsername.setError("Username (email) is required"); return false; }

        if (password.isEmpty()) { etPassword.setError("Password is required"); return false; }
        if (password.length() < 8) { etPassword.setError("Password must be at least 8 characters"); return false; }
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            etPassword.setError("Password must contain letters and numbers");
            return false;
        }

        if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Passwords do not match"); return false; }

        return true;
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String parentContact = etParentContact.getText().toString().trim();
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // user selected role (only matters after an admin exists)
        final boolean wantsAdmin = rbAdmin.isChecked();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser fu = mAuth.getCurrentUser();
                    if (fu == null) {
                        Toast.makeText(this, "Signup failed. Try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = fu.getUid();

                    final com.google.firebase.firestore.DocumentReference metaRef =
                            fs.collection("meta").document("app_state");
                    final com.google.firebase.firestore.DocumentReference userRef =
                            fs.collection("users").document(uid);

                    fs.runTransaction(transaction -> {

                        com.google.firebase.firestore.DocumentSnapshot metaSnap = transaction.get(metaRef);

                        boolean adminCreated = false;
                        if (metaSnap.exists()) {
                            Boolean b = metaSnap.getBoolean("adminCreated");
                            adminCreated = (b != null && b);
                        }

                        boolean isAdmin;
                        String status;

                        if (createdByAdmin) {
                            // Admin is adding a member inside app → active immediately
                            isAdmin = false;
                            status = "active";
                        } else if (!adminCreated) {
                            // ✅ BOOTSTRAP: first ever account becomes active admin
                            isAdmin = true;
                            status = "active";
                        } else {
                            // ✅ After admin exists: everyone needs approval
                            isAdmin = wantsAdmin;  // keep chosen role
                            status = "pending";
                        }

                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("userId", uid);
                        userMap.put("fullName", fullName);
                        userMap.put("username", email);
                        userMap.put("contact", contact);
                        userMap.put("address", address);
                        userMap.put("parentContact", parentContact);
                        userMap.put("isAdmin", isAdmin);
                        userMap.put("status", status);

                        transaction.set(userRef, userMap);

                        // If bootstrap admin, lock it
                        if (!createdByAdmin && !adminCreated && isAdmin && "active".equals(status)) {
                            Map<String, Object> metaUpdate = new HashMap<>();
                            metaUpdate.put("adminCreated", true);
                            metaUpdate.put("adminUid", uid);
                            transaction.set(metaRef, metaUpdate,
                                    com.google.firebase.firestore.SetOptions.merge());
                        }

                        return new Object[]{isAdmin, status};

                    }).addOnSuccessListener(result -> {
                        Object[] arr = (Object[]) result;
                        boolean isAdmin = (boolean) arr[0];
                        String status = (String) arr[1];

                        // Local cache (offline on this device)
                        messDb.upsertUserFromRemote(uid, fullName, email, contact, address, parentContact, isAdmin, status);
                        messDb.updateLocalPasswordByUsername(email, password);

                        if ("active".equalsIgnoreCase(status)) {
                            messDb.addOrUpdateMember(email, isAdmin ? "admin" : "member", contact, address, parentContact);
                        }

                        // KV cache
                        String base = "user_" + email + "_";
                        kvDb.insertOrUpdate(base + "name", fullName);
                        kvDb.insertOrUpdate(base + "contact", contact);
                        kvDb.insertOrUpdate(base + "address", address);
                        kvDb.insertOrUpdate(base + "parent_contact", parentContact);
                        kvDb.insertOrUpdate("username", email);
                        kvDb.insertOrUpdate("role", isAdmin ? "admin" : "member");

                        if (createdByAdmin) {
                            Toast.makeText(this, "Member added successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else if ("active".equalsIgnoreCase(status) && isAdmin) {
                            Toast.makeText(this, "Admin account created successfully!", Toast.LENGTH_SHORT).show();
                            autoLoginAdmin(uid, email, fullName);
                        } else {
                            Toast.makeText(this, "Registration done! Waiting for admin approval.", Toast.LENGTH_LONG).show();
                            finish();
                        }

                    }).addOnFailureListener(e ->
                            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void autoLoginAdmin(String userId, String emailUsername, String fullName) {

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_id", userId);
        editor.putBoolean("is_admin", true);
        editor.putString("user_name", fullName);
        editor.putBoolean("is_logged_in", true);
        editor.apply();

        kvDb.insertOrUpdate("username", emailUsername);
        kvDb.insertOrUpdate("role", "admin");
        kvDb.insertOrUpdate("is_logged_in", "true");
        kvDb.insertOrUpdate("full_name", fullName);

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("user_name", emailUsername);
        intent.putExtra("user_role", "admin");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
