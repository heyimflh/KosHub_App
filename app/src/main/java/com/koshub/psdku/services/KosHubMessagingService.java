package com.koshub.psdku.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.koshub.psdku.repositories.FCMTokenRepository;
import com.koshub.psdku.utils.NotificationHelper;

/**
 * Service to handle FCM events.
 */
public class KosHubMessagingService extends FirebaseMessagingService {
    private static final String TAG = "KosHubFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New Token: " + token);
        FCMTokenRepository.getInstance().saveToken(token, null);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Handle Notification Payload
        String title = "";
        String body = "";
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Handle Data Payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            if (title == null || title.isEmpty()) title = remoteMessage.getData().get("title");
            if (body == null || body.isEmpty()) body = remoteMessage.getData().get("body");
            
            NotificationHelper.showLocalNotification(this, title, body, remoteMessage.getData());
        } else if (remoteMessage.getNotification() != null) {
            // If only notification but no data, we still might want to show it manually 
            // if we want specific channel control, but usually system handles this when app is backgrounded.
            // For foreground control, we call helper.
            NotificationHelper.showLocalNotification(this, title, body, new java.util.HashMap<>());
        }
    }
}
