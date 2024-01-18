package com.alamterokaniaga.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.FirebaseMessagingException;

public class FCMSender {
    public static void sendPushNotification(String deviceToken, String title, String content, String groupKey,
            String category, String sender) {
        // Log relevant information
        // System.out.println("Sending push notification to deviceToken: " +
        // deviceToken);
        // System.out.println("Title: " + title);
        // System.out.println("Message: " + message);

        Message fcmMessage = Message.builder()
                .setToken(deviceToken)
                .putData("sender", title)
                .putData("title", title)
                .putData("content", content)
                .putData("category", category)
                .putData("groupKey", groupKey)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(content)
                        .build())
                .build();

        try {
            // Attempt to send the FCM message
            String response = FirebaseMessaging.getInstance().send(fcmMessage);
            System.out.println("Successfully sent message. FCM Response: " + response);
        } catch (FirebaseMessagingException e) {
            // Handle FCM-specific exceptions
            e.printStackTrace();
            System.out.println("Failed to send message. FCM Response: " + e.getLocalizedMessage());
        } catch (Exception e) {
            // Handle other exceptions
            e.printStackTrace();
        }
    }
}
