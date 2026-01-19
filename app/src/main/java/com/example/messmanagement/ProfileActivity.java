package com.example.messmanagement;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivBack;

    // ✅ Photo (local only)
    private ImageView ivProfilePhoto;
    private Button btnUploadPhoto;

    private TextView tvProfileName, tvProfileRole, tvProfileContact, tvProfileAddress, tvProfileParentContact;
    private Button btnSync, btnLogout, btnAdminPanel;

    private KeyValueDB kvDb;
    private MessDBHelper messDb;

    private FirebaseAuth mAuth;
    private FirebaseFirestore fs;

    private String username; // email
    private String role;     // "admin"/"member"

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        kvDb = new KeyValueDB(this);
        messDb = new MessDBHelper(this);

        mAuth = FirebaseAuth.getInstance();
        fs = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvProfileContact = findViewById(R.id.tvProfileContact);
        tvProfileAddress = findViewById(R.id.tvProfileAddress);
        tvProfileParentContact = findViewById(R.id.tvProfileParentContact);

        btnSync = findViewById(R.id.btnSync);
        btnLogout = findViewById(R.id.btnLogout);
        btnAdminPanel = findViewById(R.id.btnAdminPanel);

        ivBack.setOnClickListener(v -> finish());

        // 1) Get username/role
        username = getIntent().getStringExtra("user_name");
        role = getIntent().getStringExtra("user_role");

        if (username == null) username = kvDb.getValueByKey("username");
        if (role == null) role = kvDb.getValueByKey("role");

        if (username == null) username = "Guest";
        if (role == null) role = "member";

        // ✅ Image picker (no permission needed)
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    String savedPath = saveImageToInternalStorage(uri, username);
                    if (savedPath != null) {
                        // show instantly
                        ivProfilePhoto.setImageURI(Uri.fromFile(new File(savedPath)));

                        // cache path
                        String base = "user_" + username + "_";
                        kvDb.insertOrUpdate(base + "photo_path", savedPath);

                        Toast.makeText(this, "Photo saved (local)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnUploadPhoto.setOnClickListener(v -> {
            if ("Guest".equals(username)) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            pickImageLauncher.launch("image/*");
        });

        // 2) Show cached local values
        loadFromLocalCache(username, role);

        // 3) Fetch latest from Firestore (profile info only, not photo)
        fetchFromFirestoreAndUpdateUI();

        // Admin button
        if ("admin".equalsIgnoreCase(role)) {
            btnAdminPanel.setVisibility(Button.VISIBLE);
            btnAdminPanel.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminPanelActivity.class))
            );
        } else {
            btnAdminPanel.setVisibility(Button.GONE);
        }

        btnSync.setOnClickListener(v -> syncToFirestore());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            kvDb.deleteDataByKey("username");
            kvDb.deleteDataByKey("role");

            // optional: keep photo even after logout (comment next line if you want to delete it)
            // deleteLocalPhotoIfExists(username);

            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void loadFromLocalCache(String username, String role) {
        String base = "user_" + username + "_";

        String fullName = kvDb.getValueByKey(base + "name");
        String contact = kvDb.getValueByKey(base + "contact");
        String address = kvDb.getValueByKey(base + "address");
        String parentContact = kvDb.getValueByKey(base + "parent_contact");

        // ✅ photo path
        String photoPath = kvDb.getValueByKey(base + "photo_path");

        if (fullName == null) fullName = username;

        tvProfileName.setText(fullName);
        tvProfileRole.setText(role.equalsIgnoreCase("admin") ? "Admin" : "Member");
        tvProfileContact.setText("Contact: " + (contact != null ? contact : "N/A"));
        tvProfileAddress.setText("Address: " + (address != null ? address : "N/A"));
        tvProfileParentContact.setText("Parents' Contact: " + (parentContact != null ? parentContact : "N/A"));

        // ✅ show photo
        if (photoPath != null && !photoPath.trim().isEmpty()) {
            File f = new File(photoPath);
            if (f.exists()) {
                ivProfilePhoto.setImageURI(Uri.fromFile(f));
            } else {
                ivProfilePhoto.setImageResource(R.drawable.ic_person);
            }
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_person);
        }
    }

    private void fetchFromFirestoreAndUpdateUI() {
        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu == null) return;

        final String uid = fu.getUid();

        fs.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String fullName = doc.getString("fullName");
                    String email = doc.getString("username");
                    String contact = doc.getString("contact");
                    String address = doc.getString("address");
                    String parentContact = doc.getString("parentContact");

                    Boolean isAdminBool = doc.getBoolean("isAdmin");
                    boolean isAdmin = isAdminBool != null && isAdminBool;

                    String status = doc.getString("status");
                    if (status == null) status = "active";

                    if (email == null) email = fu.getEmail();
                    if (email == null) email = username;

                    String showName = (fullName != null && !fullName.isEmpty()) ? fullName : email;

                    tvProfileName.setText(showName);
                    tvProfileRole.setText(isAdmin ? "Admin" : "Member");
                    tvProfileContact.setText("Contact: " + (contact != null ? contact : "N/A"));
                    tvProfileAddress.setText("Address: " + (address != null ? address : "N/A"));
                    tvProfileParentContact.setText("Parents' Contact: " + (parentContact != null ? parentContact : "N/A"));

                    // Cache locally
                    String base = "user_" + email + "_";
                    kvDb.insertOrUpdate(base + "name", showName);
                    kvDb.insertOrUpdate(base + "contact", contact != null ? contact : "");
                    kvDb.insertOrUpdate(base + "address", address != null ? address : "");
                    kvDb.insertOrUpdate(base + "parent_contact", parentContact != null ? parentContact : "");
                    kvDb.insertOrUpdate("username", email);
                    kvDb.insertOrUpdate("role", isAdmin ? "admin" : "member");

                    messDb.upsertUserFromRemote(uid, showName, email,
                            contact, address, parentContact, isAdmin, status);

                    // Update role + admin button
                    role = isAdmin ? "admin" : "member";
                    if ("admin".equalsIgnoreCase(role)) {
                        btnAdminPanel.setVisibility(Button.VISIBLE);
                        btnAdminPanel.setOnClickListener(v ->
                                startActivity(new Intent(this, AdminPanelActivity.class))
                        );
                    } else {
                        btnAdminPanel.setVisibility(Button.GONE);
                    }
                });
    }

    private void syncToFirestore() {
        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        if (fu == null) {
            Toast.makeText(this, "Login online first to sync.", Toast.LENGTH_LONG).show();
            return;
        }

        final String uid = fu.getUid();
        final String email = username;

        String base = "user_" + email + "_";
        final String fullName = kvDb.getValueByKey(base + "name") != null ? kvDb.getValueByKey(base + "name") : email;
        final String contact = kvDb.getValueByKey(base + "contact") != null ? kvDb.getValueByKey(base + "contact") : "";
        final String address = kvDb.getValueByKey(base + "address") != null ? kvDb.getValueByKey(base + "address") : "";
        final String parentContact = kvDb.getValueByKey(base + "parent_contact") != null ? kvDb.getValueByKey(base + "parent_contact") : "";

        final boolean isAdmin = "admin".equalsIgnoreCase(role);

        String tmpStatus = messDb.getUserStatusByUsername(email);
        final String status = (tmpStatus == null) ? "active" : tmpStatus;

        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("userId", uid);
        userMap.put("username", email);
        userMap.put("fullName", fullName);
        userMap.put("contact", contact);
        userMap.put("address", address);
        userMap.put("parentContact", parentContact);
        userMap.put("isAdmin", isAdmin);
        userMap.put("status", status);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(userMap)
                .addOnSuccessListener(unused -> {
                    messDb.upsertUserFromRemote(uid, fullName, email, contact, address, parentContact, isAdmin, status);
                    kvDb.insertOrUpdate("role", isAdmin ? "admin" : "member");
                    Toast.makeText(this, "Synced to Firebase", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ✅ Save selected image into app internal storage (private)
    private String saveImageToInternalStorage(Uri uri, String username) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;

            File dir = new File(getFilesDir(), "profile_photos");
            if (!dir.exists()) dir.mkdirs();

            // stable file per user (overwrite old)
            String safe = username.replaceAll("[^a-zA-Z0-9._-]", "_");
            File outFile = new File(dir, safe + "_profile.jpg");

            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            fos.close();
            is.close();

            return outFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Optional: delete local photo when logging out (if you want)
    private void deleteLocalPhotoIfExists(String username) {
        try {
            String base = "user_" + username + "_";
            String path = kvDb.getValueByKey(base + "photo_path");
            if (path != null) {
                File f = new File(path);
                if (f.exists()) f.delete();
                kvDb.insertOrUpdate(base + "photo_path", "");
            }
        } catch (Exception ignored) {}
    }
}
