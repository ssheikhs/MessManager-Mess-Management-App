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

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "mess_notifications_channel";
    private static final String CHANNEL_NAME = "Mess Notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // 1) Prefer data payload (Cloud Functions / Worker sends data)
        Map<String, String> data = remoteMessage.getData();

        String title = "Mess Management";
        String body = "You have a new notification.";
        String type = "";
        String month = "";
        String senderName = "";

        if (data != null && !data.isEmpty()) {
            if (data.get("title") != null) title = data.get("title");
            if (data.get("body") != null) body = data.get("body");
            if (data.get("type") != null) type = data.get("type"); // meal/payment/expense
            if (data.get("month") != null) month = data.get("month");
            if (data.get("senderName") != null) senderName = data.get("senderName");
        }

        // 2) Fallback to notification payload
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        showNotification(title, body, type, month, senderName);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Optional: send token to your own server if needed
    }

    private void showNotification(String title, String body, String type, String month, String senderName) {

        // Android 13+ runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // permission not granted
            }
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Mess Management app notifications");
                nm.createNotificationChannel(channel);
            }
        }

        // Default open Dashboard (you can route by type later)
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Pass extras (optional)
        intent.putExtra("notif_type", type);
        intent.putExtra("notif_month", month);
        intent.putExtra("notif_senderName", senderName);

        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String safeTitle = (title == null || title.trim().isEmpty()) ? "Mess Management" : title;
        String safeBody = (body == null) ? "" : body;

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(safeTitle)
                .setContentText(safeBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(safeBody))
                .setAutoCancel(true)
                .setSound(sound)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        int notificationId = (int) (System.currentTimeMillis() & 0x7fffffff);
        nm.notify(notificationId, nb.build());
    }
}
