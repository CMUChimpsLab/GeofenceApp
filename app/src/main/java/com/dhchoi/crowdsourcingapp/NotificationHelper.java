package com.dhchoi.crowdsourcingapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

public class NotificationHelper {

    private NotificationHelper() {
    }

    /**
     * Create and show notification
     * @param title             title
     * @param message           message
     * @param tag               TAG
     * @param context           context
     * @param targetActivity    where to go
     * @param taskId            optional
     */
    public static void createNotification(String title, String message, String tag, Context context, Class targetActivity, @Nullable String taskId) {
        Intent intent = new Intent(context, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (taskId != null)
            intent.putExtra("taskId", taskId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(tag, 0 /* ID of notification */, notificationBuilder.build());
    }

    /**
     * Create and show notification
     * @param title             title
     * @param message           message
     * @param context           context
     * @param targetActivity    where to go
     */
    public static void createNotification(String title, String message, Context context, Class targetActivity) {
        createNotification(title, message, "", context, targetActivity, null);
    }

    /**
     * Create and show notification
     * @param title             title
     * @param message           message
     * @param context           context
     * @param targetActivity    where to go
     * @param taskId            optional
     */
    public static void createNotification(String title, String message, Context context, Class targetActivity, @Nullable String taskId) {
        createNotification(title, message, "", context, targetActivity, taskId);
    }

    /**
     * Create and show notification
     * @param title             title
     * @param message           message
     * @param context           context
     * @param link              download link
     */
    public static void createDownloadNotification(String title, String message, Context context, String link) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
//        intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 26, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(null, 26, notificationBuilder.build());
    }
}
