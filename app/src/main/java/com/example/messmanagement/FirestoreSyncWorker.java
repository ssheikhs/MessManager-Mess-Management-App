package com.example.messmanagement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreSyncWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "mess_firestore_sync_worker";

    private final MessDBHelper messDb;
    private final FirebaseFirestore fs;

    public FirestoreSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        messDb = new MessDBHelper(context);
        fs = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // ===================== 1) MEALS =====================
            // IMPORTANT: this requires you to store which meal changed last.
            // pm.changedMealType = "Breakfast"/"Lunch"/"Dinner"
            // pm.changedValue    = 1 or 0
            List<PendingMeal> pendingMeals = messDb.getPendingMeals();
            for (PendingMeal pm : pendingMeals) {
                if (pm.memberName == null || pm.date == null) continue;

                String docId = pm.memberName + "_" + pm.date;

                Map<String, Object> mealMap = new HashMap<>();
                mealMap.put("memberName", pm.memberName);
                mealMap.put("date", pm.date);
                mealMap.put("breakfast", pm.breakfast);
                mealMap.put("lunch", pm.lunch);
                mealMap.put("dinner", pm.dinner);

                Tasks.await(fs.collection("meals_daily").document(docId).set(mealMap));
                messDb.markMealSynced(pm.memberName, pm.date);

                // ✅ Notify only if this sync was triggered by a specific meal ADD (value=1)
                if (pm.changedMealType != null && pm.changedValue == 1) {
                    createMealNotification(
                            pm.memberName,
                            pm.date,
                            pm.changedMealType
                    );
                }
            }

            // ===================== 2) EXPENSES =====================
            List<PendingExpense> pendingExpenses = messDb.getPendingExpenses();
            for (PendingExpense pe : pendingExpenses) {
                if (pe.remoteId == null || pe.remoteId.trim().isEmpty()) continue;

                Map<String, Object> exp = new HashMap<>();
                exp.put("date", pe.date);
                exp.put("title", pe.title);
                exp.put("category", pe.category);
                exp.put("paidBy", pe.paidBy);
                exp.put("amount", pe.amount);

                Tasks.await(fs.collection("expenses").document(pe.remoteId).set(exp));
                messDb.markExpenseSynced(pe.remoteId);

                // ✅ Notify for every expense (including payments)
                createExpenseNotification(pe);
            }

            return Result.success();

        } catch (Exception e) {
            return Result.retry();
        }
    }

    // ===================== NOTIFICATION HELPERS =====================

    private void createMealNotification(String memberEmail, String date, String mealType) throws Exception {
        String displayName = messDb.getDisplayNameByUsername(memberEmail);
        if (displayName == null || displayName.trim().isEmpty()) displayName = memberEmail;

        String month = (date != null && date.length() >= 7) ? date.substring(0, 7) : "";
        String mealKey = mealType.toLowerCase(Locale.getDefault());

        Map<String, Object> n = new HashMap<>();
        n.put("type", "meal");
        n.put("title", mealType + " Added");
        n.put("body", displayName + " added " + mealType + " (" + date + ")");
        n.put("senderName", displayName);      // ✅ for UI/FCM
        n.put("senderEmail", memberEmail);     // optional
        n.put("date", date);
        n.put("month", month);
        n.put("mealType", mealKey);
        n.put("createdAt", FieldValue.serverTimestamp());

        // deterministic id prevents duplicates
        String nid = "meal_" + memberEmail + "_" + date + "_" + mealKey;
        Tasks.await(fs.collection("notifications").document(nid).set(n));
    }

    private void createExpenseNotification(PendingExpense pe) throws Exception {
        String paidByEmail = (pe.paidBy == null) ? "" : pe.paidBy;
        String displayName = messDb.getDisplayNameByUsername(paidByEmail);
        if (displayName == null || displayName.trim().isEmpty()) displayName = paidByEmail;

        String date = (pe.date == null) ? "" : pe.date;
        String month = (date.length() >= 7) ? date.substring(0, 7) : "";

        String category = (pe.category == null) ? "" : pe.category;
        boolean isPayment = MessDBHelper.CATEGORY_PAYMENT.equalsIgnoreCase(category);

        String safeTitle = (pe.title == null) ? "" : pe.title;

        String body;
        String title;
        String type;

        if (isPayment) {
            type = "payment";
            title = "Payment Added";
            body = displayName + " paid " + pe.amount + "৳ (" + date + ")";
        } else {
            type = "expense";
            title = "Expense Added";
            body = displayName + " added expense: " + safeTitle +
                    " — " + pe.amount + "৳ (" + category + ", " + date + ")";
        }

        Map<String, Object> n = new HashMap<>();
        n.put("type", type);
        n.put("title", title);
        n.put("body", body);

        n.put("senderName", displayName);      // ✅ for UI/FCM
        n.put("senderEmail", paidByEmail);

        n.put("date", date);
        n.put("month", month);
        n.put("amount", String.valueOf(pe.amount));
        n.put("category", category);
        n.put("expenseTitle", safeTitle);
        n.put("createdAt", FieldValue.serverTimestamp());

        // one notification per expense
        String nid = "exp_" + pe.remoteId;
        Tasks.await(fs.collection("notifications").document(nid).set(n));
    }

    // ===================== WORK CONSTRAINTS =====================

    public static Constraints networkConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }
}
