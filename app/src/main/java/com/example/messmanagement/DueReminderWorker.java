package com.example.messmanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DueReminderWorker extends Worker {

    private static final String CHANNEL_ID = "mess_reminders_channel";
    private static final int NOTIFICATION_ID = 1001;

    public DueReminderWorker(@NonNull Context context,
                             @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        KeyValueDB kvDb = new KeyValueDB(ctx);
        String username = kvDb.getValueByKey("username");
        if (username == null || "Guest".equals(username)) {
            return Result.success();
        }

        MessDBHelper messDb = new MessDBHelper(ctx);

        // 1) Check current month due from local DB
        String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());
        ExpenseBreakdown breakdown =
                messDb.getMemberExpenseBreakdown(username, monthPrefix);

        boolean hasDue = breakdown != null && breakdown.balance > 0;

        // 2) Check if today's meals are empty (user forgot to add meals)
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        int[] todayMeals = messDb.getMealsForDate(username, today);
        boolean noMealsToday = (todayMeals[0] + todayMeals[1] + todayMeals[2]) == 0;

        // If nothing to remind, just exit.
        if (!hasDue && !noMealsToday) {
            return Result.success();
        }

        String message;
        if (hasDue && noMealsToday) {
            message = "You have due this month and no meals entered today. Please update.";
        } else if (hasDue) {
            message = "You still have pending mess due this month. Please pay soon.";
        } else {
            message = "You have not added today's meals yet. Tap to update.";
        }

        createNotificationChannel(ctx);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // your app icon
                .setContentTitle("Mess Reminder")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // 🔒 Permission check for Android 13+ (POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int perm = ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.POST_NOTIFICATIONS
            );
            if (perm != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted -> don't crash, just skip notification
                return Result.success();
            }
        }

        NotificationManagerCompat manager = NotificationManagerCompat.from(ctx);
        manager.notify(NOTIFICATION_ID, builder.build());

        return Result.success();
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Mess Reminders";
            String description = "Reminders for due payments and missing meal entries";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
