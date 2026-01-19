package com.example.messmanagement;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SyncScheduler {

    // Call once (e.g., Dashboard onCreate / Application onCreate)
    public static void startPeriodicSync(Context context) {
        Constraints constraints = FirestoreSyncWorker.networkConstraints();

        PeriodicWorkRequest periodic =
                new PeriodicWorkRequest.Builder(FirestoreSyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                FirestoreSyncWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic
        );
    }

    // Call when user presses “Sync” button or after login
    public static void runOneTimeSyncNow(Context context) {
        Constraints constraints = FirestoreSyncWorker.networkConstraints();

        OneTimeWorkRequest oneTime =
                new OneTimeWorkRequest.Builder(FirestoreSyncWorker.class)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                FirestoreSyncWorker.UNIQUE_WORK_NAME + "_now",
                ExistingWorkPolicy.REPLACE,
                oneTime
        );
    }
}
