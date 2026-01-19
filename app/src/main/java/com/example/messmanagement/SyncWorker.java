package com.example.messmanagement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        MessDBHelper db = new MessDBHelper(getApplicationContext());
        FirebaseFirestore fs = FirebaseFirestore.getInstance();

        List<PendingExpense> pending = db.getPendingExpenses();
        if (pending.isEmpty()) return Result.success();

        // NOTE: Worker is synchronous; Firestore is async.
        // Easiest reliable approach: upload one-by-one using Tasks.await (needs play-services-tasks)
        // If you don't want that dependency, we can switch to ListenableWorker/CoroutineWorker.
        try {
            for (PendingExpense p : pending) {
                Map<String, Object> map = new HashMap<>();
                map.put("title", p.title);
                map.put("amount", p.amount);
                map.put("category", p.category);
                map.put("paidBy", p.paidBy);
                map.put("date", p.date);

                // write to fixed doc id => no duplicates
                com.google.android.gms.tasks.Tasks.await(
                        fs.collection("expenses").document(p.remoteId).set(map)
                );

                db.markExpenseSynced(p.remoteId);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
