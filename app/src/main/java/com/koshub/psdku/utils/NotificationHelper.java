package com.koshub.psdku.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.koshub.psdku.NotificationActivity;
import com.koshub.psdku.OwnerBookingActivity;
import com.koshub.psdku.OwnerChatRoomActivity;
import com.koshub.psdku.OwnerFinanceReportActivity;
import com.koshub.psdku.R;
import com.koshub.psdku.WaitingListQueueActivity;

import java.util.Map;

/**
 * Helper to manage Notification Channels and local notifications.
 */
public class NotificationHelper {

    public static final String CHANNEL_GENERAL = "koshub_general";
    public static final String CHANNEL_BOOKING = "koshub_booking";
    public static final String CHANNEL_CHAT = "koshub_chat";
    public static final String CHANNEL_COMPLAINT = "koshub_complaint";
    public static final String CHANNEL_FINANCE = "koshub_finance";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel generalChannel = new NotificationChannel(CHANNEL_GENERAL, "Umum", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel bookingChannel = new NotificationChannel(CHANNEL_BOOKING, "Booking", NotificationManager.IMPORTANCE_HIGH);
            NotificationChannel chatChannel = new NotificationChannel(CHANNEL_CHAT, "Chat", NotificationManager.IMPORTANCE_HIGH);
            NotificationChannel complaintChannel = new NotificationChannel(CHANNEL_COMPLAINT, "Komplain", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel financeChannel = new NotificationChannel(CHANNEL_FINANCE, "Keuangan", NotificationManager.IMPORTANCE_DEFAULT);

            manager.createNotificationChannel(generalChannel);
            manager.createNotificationChannel(bookingChannel);
            manager.createNotificationChannel(chatChannel);
            manager.createNotificationChannel(complaintChannel);
            manager.createNotificationChannel(financeChannel);
        }
    }

    public static void showLocalNotification(Context context, String title, String body, Map<String, String> data) {
        String channelId = CHANNEL_GENERAL;
        String type = data.get("targetType");
        
        if (DatabaseConstants.TARGET_CHAT.equals(type)) channelId = CHANNEL_CHAT;
        else if (DatabaseConstants.TARGET_OWNER_BOOKING.equals(type) || DatabaseConstants.TARGET_WAITING_LIST.equals(type)) channelId = CHANNEL_BOOKING;
        else if (DatabaseConstants.TARGET_COMPLAINT.equals(type)) channelId = CHANNEL_COMPLAINT;
        else if (DatabaseConstants.TARGET_FINANCE.equals(type) || DatabaseConstants.TARGET_WITHDRAW.equals(type)) channelId = CHANNEL_FINANCE;

        Intent intent = buildTargetIntent(context, type, data.get("targetId"), data);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_logo_koshub)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private static Intent buildTargetIntent(Context context, String targetType, String targetId, Map<String, String> data) {
        Intent intent;
        if (DatabaseConstants.TARGET_OWNER_BOOKING.equals(targetType)) {
            intent = new Intent(context, OwnerBookingActivity.class);
        } else if (DatabaseConstants.TARGET_WAITING_LIST.equals(targetType)) {
            intent = new Intent(context, WaitingListQueueActivity.class);
            intent.putExtra("BOOKING_ID", targetId);
        } else if (DatabaseConstants.TARGET_CHAT.equals(targetType)) {
            intent = new Intent(context, OwnerChatRoomActivity.class);
            intent.putExtra("CHAT_ID", targetId);
        } else if (DatabaseConstants.TARGET_FINANCE.equals(targetType) || DatabaseConstants.TARGET_WITHDRAW.equals(targetType)) {
            intent = new Intent(context, OwnerFinanceReportActivity.class);
        } else {
            intent = new Intent(context, NotificationActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }
}
