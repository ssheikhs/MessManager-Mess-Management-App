package com.example.messmanagement;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ApproveMembersActivity extends AppCompatActivity {

    private ListView lvPendingMembers;
    private TextView tvEmpty;

    private MessDBHelper messDb;
    private FirebaseFirestore fs;

    private List<User> pendingUsers = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_members);

        messDb = new MessDBHelper(this);
        fs = FirebaseFirestore.getInstance();

        lvPendingMembers = findViewById(R.id.lvPendingMembers);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.ivBack).setOnClickListener(v -> onBackPressed());

        lvPendingMembers.setEmptyView(tvEmpty);
        lvPendingMembers.setOnItemClickListener((parent, view, position, id) ->
                showApproveDialog(position)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isOnline()) syncPendingFromFirestoreThenLoad();
        else loadPendingMembersFromSQLite();
    }

    private String roleLabel(User u) {
        return (u != null && u.isAdmin()) ? "Admin" : "Member";
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network n = cm.getActiveNetwork();
            if (n == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(n);
            return caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
    }

    private void syncPendingFromFirestoreThenLoad() {
        fs.collection("users")
                .whereEqualTo("status", "pending")
                // ✅ removed: .whereEqualTo("isAdmin", false)
                .get()
                .addOnSuccessListener(qs -> {
                    for (QueryDocumentSnapshot doc : qs) {
                        String uid = doc.getId(); // document id = UID
                        String fullName = doc.getString("fullName");
                        String email = doc.getString("username");

                        String contact = doc.getString("contact");
                        String address = doc.getString("address");
                        String parentContact = doc.getString("parentContact");

                        Boolean isAdminBool = doc.getBoolean("isAdmin");
                        boolean isAdmin = isAdminBool != null && isAdminBool;

                        String status = doc.getString("status");
                        if (status == null) status = "pending";

                        if (email != null && !email.trim().isEmpty()) {
                            messDb.upsertUserFromRemote(uid, fullName, email, contact, address, parentContact, isAdmin, status);
                        }
                    }
                    loadPendingMembersFromSQLite();
                })
                .addOnFailureListener(e -> loadPendingMembersFromSQLite());
    }

    private void loadPendingMembersFromSQLite() {
        pendingUsers = messDb.getPendingMembers(); // ✅ now should return ALL pending (admin + member)

        List<String> displayList = new ArrayList<>();
        for (User u : pendingUsers) {
            displayList.add(u.getFullName() + " (" + u.getUsername() + ") - " + roleLabel(u));
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        lvPendingMembers.setAdapter(adapter);
    }

    private void showApproveDialog(int position) {
        if (position < 0 || position >= pendingUsers.size()) return;

        User user = pendingUsers.get(position);
        String uid = user.getUserId();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pending user");
        builder.setMessage("What do you want to do with:\n\n" +
                user.getFullName() + "\n" +
                user.getUsername() + "\n" +
                "Role: " + roleLabel(user));

        builder.setPositiveButton("Approve", (dialog, which) -> {
            if (!isOnline()) {
                Toast.makeText(this, "Internet required to approve (cloud update).", Toast.LENGTH_SHORT).show();
                return;
            }

            fs.collection("users")
                    .document(uid)
                    .update("status", "active")
                    .addOnSuccessListener(unused -> {
                        boolean ok = messDb.approvePendingMember(uid);
                        Toast.makeText(this,
                                ok ? "User approved" : "Approved in cloud, local update failed",
                                Toast.LENGTH_SHORT).show();
                        loadPendingMembersFromSQLite();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Approve failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        builder.setNegativeButton("Reject", (dialog, which) -> {
            if (!isOnline()) {
                Toast.makeText(this, "Internet required to reject (cloud update).", Toast.LENGTH_SHORT).show();
                return;
            }

            fs.collection("users")
                    .document(uid)
                    .update("status", "deleted")
                    .addOnSuccessListener(unused -> {
                        boolean ok = messDb.deleteUserById(uid);
                        Toast.makeText(this,
                                ok ? "User rejected" : "Rejected in cloud, local delete failed",
                                Toast.LENGTH_SHORT).show();
                        loadPendingMembersFromSQLite();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Reject failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }
}
