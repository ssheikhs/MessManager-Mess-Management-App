package com.example.messmanagement;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class NotificationUtils {

    public static final String CHANNEL_ID = "mess_notifications_channel";
    private static final String CHANNEL_NAME = "Mess Notifications";

    private static void ensureChannel(Context context) {
        if (context == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing != null) return;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Mess Management app notifications");
            nm.createNotificationChannel(channel);
        }
    }

    // ✅ keep your old call working (random id)
    public static void showLocal(Context context, String title, String body) {
        int notificationId = (int) (System.currentTimeMillis() & 0x7fffffff);
        showLocal(context, title, body, notificationId);
    }

    // ✅ NEW: stable-id notification (recommended)
    public static void showLocal(Context context, String title, String body, int notificationId) {
        if (context == null) return;

        // Android 13+ runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        ensureChannel(context);

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent intent = new Intent(context, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                1001, // ✅ fixed requestCode (not 0)
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String safeTitle = (title == null || title.trim().isEmpty()) ? "Mess Management" : title;
        String safeBody = (body == null) ? "" : body;

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(safeTitle)
                .setContentText(safeBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(safeBody))
                .setAutoCancel(true)
                .setSound(sound)
                .setContentIntent(pi)
                .setOnlyAlertOnce(true) // ✅ avoid repeated sound/vibrate if updated
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        nm.notify(notificationId, nb.build());
    }

    // ✅ helper: convert Firestore docId to stable int
    public static int stableIdFromString(String s) {
        if (s == null) return 0;
        return (s.hashCode() & 0x7fffffff);
    }
}
