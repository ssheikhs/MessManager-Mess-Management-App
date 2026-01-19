package com.example.messmanagement;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MemberListActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ListView lvMembers;

    private MessDBHelper messDb;
    private KeyValueDB kvDb;

    private List<Member> memberList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private String mode;   // null / "remove" / "approve"
    private String currentRole; // "admin" or "member"

    // Firebase
    private FirebaseFirestore fs;
    private FirebaseAuth mAuth;
    private ListenerRegistration usersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);

        messDb = new MessDBHelper(this);
        kvDb = new KeyValueDB(this);

        fs = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ivBack = findViewById(R.id.ivBack);
        lvMembers = findViewById(R.id.lvMembers);

        ivBack.setOnClickListener(v -> finish());

        // role from KV (works even if intent not passed)
        currentRole = kvDb.getValueByKey("role");
        if (currentRole == null) currentRole = "member";

        // mode comes from AdminPanelActivity
        mode = getIntent().getStringExtra("mode");

        // ✅ Only admin is allowed to use approve/remove modes
        if (!"admin".equalsIgnoreCase(currentRole)) {
            mode = null; // force normal view mode for members
        }

        loadMembersFromLocal();
        setupClickListener();
        attachUsersListenerIfSignedIn();
    }

    // ========================== FIRESTORE LISTENER ==========================

    private void attachUsersListenerIfSignedIn() {
        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu == null) {
            // Not logged in to FirebaseAuth → do NOT attach listener
            // (prevents PERMISSION_DENIED + random behavior)
            return;
        }

        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }

        usersListener = fs.collection("users")
                .addSnapshotListener((@Nullable QuerySnapshot value,
                                      @Nullable FirebaseFirestoreException error) -> {

                    if (error != null || value == null) {
                        Toast.makeText(MemberListActivity.this,
                                "Online member sync failed, using local list.",
                                Toast.LENGTH_SHORT).show();
                        loadMembersFromLocal();
                        return;
                    }

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId == null || userId.trim().isEmpty()) userId = doc.getId();

                        String username = doc.getString("username"); // email
                        if (username == null || username.trim().isEmpty()) continue;

                        String fullName = doc.getString("fullName");
                        if (fullName == null || fullName.trim().isEmpty()) fullName = username;

                        String contact = doc.getString("contact");
                        String address = doc.getString("address");
                        String parentContact = doc.getString("parentContact");

                        Boolean isAdminBool = doc.getBoolean("isAdmin");
                        boolean isAdmin = (isAdminBool != null && isAdminBool);

                        String status = doc.getString("status");
                        if (status == null) status = "active";

                        // 1) Cache in local USERS table
                        messDb.upsertUserFromRemote(
                                userId,
                                fullName,
                                username,
                                contact,
                                address,
                                parentContact,
                                isAdmin,
                                status
                        );

                        // 2) Keep MEMBERS table in-sync (so list shows correct)
                        if ("active".equalsIgnoreCase(status)) {
                            messDb.addOrUpdateMember(
                                    username,
                                    isAdmin ? "admin" : "member",
                                    contact,
                                    address,
                                    parentContact
                            );
                        } else if ("deleted".equalsIgnoreCase(status)) {
                            messDb.deleteMemberByName(username);
                        }
                        // pending -> do nothing (not shown in members list)
                    }

                    loadMembersFromLocal();
                });
    }

    // ====================== LOCAL UI BINDING ======================

    private void loadMembersFromLocal() {
        memberList = messDb.getAllMembers();

        List<String> names = new ArrayList<>();
        if (memberList == null || memberList.isEmpty()) {
            names.add("No members found");
        } else {
            for (Member m : memberList) {
                String role = (m.role == null) ? "member" : m.role;
                names.add(m.name + " (" + role + ")");
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        lvMembers.setAdapter(adapter);
    }

    // =========================== ITEM CLICK LOGIC ===========================

    private void setupClickListener() {
        lvMembers.setOnItemClickListener((parent, view, position, id) -> {
            if (memberList == null || memberList.isEmpty()
                    || position < 0 || position >= memberList.size()) {
                return;
            }

            Member selected = memberList.get(position);
            String selectedName = selected.name; // username/email

            if ("remove".equals(mode)) {
                // admin only (mode already nulled for members, but double safety)
                if (!"admin".equalsIgnoreCase(currentRole)) {
                    Toast.makeText(this, "Only admin can remove members", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmRemove(selectedName);

            } else if ("approve".equals(mode)) {
                if (!"admin".equalsIgnoreCase(currentRole)) {
                    Toast.makeText(this, "Only admin can approve members", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmApprove(selectedName);

            } else {
                // ✅ Everyone (admin + member) can open details
                Intent intent = new Intent(this, MemberDetailsActivity.class);
                intent.putExtra("member_name", selectedName);
                startActivity(intent);
            }
        });
    }

    // ============================ REMOVE MEMBER ============================

    private void confirmRemove(String username) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove " + username + " from the mess?")
                .setPositiveButton("Remove", (dialog, which) -> {

                    messDb.deleteMemberByName(username);
                    messDb.deactivateUserByUsername(username);

                    FirebaseUser fu = mAuth.getCurrentUser();
                    if (fu != null) {
                        fs.collection("users")
                                .whereEqualTo("username", username)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    for (DocumentSnapshot d : qs.getDocuments()) {
                                        d.getReference().update("status", "deleted");
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Remote remove failed (offline?)",
                                        Toast.LENGTH_SHORT).show());
                    }

                    loadMembersFromLocal();
                    Toast.makeText(this, "Removed " + username, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ============================ APPROVE MEMBER ============================

    private void confirmApprove(String username) {
        new AlertDialog.Builder(this)
                .setTitle("Approve Member")
                .setMessage("Approve " + username + " as active member?")
                .setPositiveButton("Approve", (dialog, which) -> {

                    messDb.approveMemberByName(username);

                    FirebaseUser fu = mAuth.getCurrentUser();
                    if (fu != null) {
                        fs.collection("users")
                                .whereEqualTo("username", username)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    for (DocumentSnapshot d : qs.getDocuments()) {
                                        d.getReference().update("status", "active");
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Remote approve failed (offline?)",
                                        Toast.LENGTH_SHORT).show());
                    }

                    loadMembersFromLocal();
                    Toast.makeText(this, "Approved " + username, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ============================== LIFECYCLE ==============================

    @Override
    protected void onResume() {
        super.onResume();

        // refresh role
        currentRole = kvDb.getValueByKey("role");
        if (currentRole == null) currentRole = "member";

        // re-check mode permission
        if (!"admin".equalsIgnoreCase(currentRole)) mode = null;

        loadMembersFromLocal();
        attachUsersListenerIfSignedIn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }
}
