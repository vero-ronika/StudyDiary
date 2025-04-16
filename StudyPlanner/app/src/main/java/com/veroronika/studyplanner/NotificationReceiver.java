package com.veroronika.studyplanner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "REMINDER_CHANNEL";
    private static final String PREFS_NAME = "AppPreferences";
    private static final String PREF_NOTIFICATIONS_ENABLED = "notificationsEnabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationReceiver", "Notification received!");

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATIONS_ENABLED, true);

        if (!notificationsEnabled) {
            Log.d("NotificationReceiver", "Notifications are disabled in preferences.");
            return;
        }

        String textReminder = intent.getStringExtra("textReminder");
        String subjectName = intent.getStringExtra("subjectName");

        String notificationTitle = "Notification for " + subjectName;
        String notificationText = textReminder + " for " + subjectName + " is coming near. Study now!";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel(notificationManager);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);  

        Log.d("NotificationReceiver", "Building notification: " + notificationTitle + " - " + notificationText);

        notificationManager.notify(subjectName.hashCode(), builder.build());

        long notificationTime = System.currentTimeMillis();
        saveNotificationToHistory(context, subjectName, textReminder, notificationTime);
    }


    private void createNotificationChannel(NotificationManager notificationManager) {
        if (notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reminder Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for Reminder Notifications");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void saveNotificationToHistory(Context context, String title, String text, long notificationTime) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NotificationHistory", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(notificationTime));

        String historyEntry = "You received a notification for " + text + ", " + title + " at " + formattedDate;

        String currentHistory = sharedPreferences.getString("history", "");
        String updatedHistory = currentHistory + "\n" + historyEntry;

        editor.putString("history", updatedHistory);
        editor.apply();
    }


}
