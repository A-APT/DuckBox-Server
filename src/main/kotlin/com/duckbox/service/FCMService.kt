package com.duckbox.service

import com.duckbox.dto.notification.NotificationMessage
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushNotification
import org.springframework.stereotype.Service

@Service
class FCMService {
    fun sendNotification(notification: NotificationMessage, isTopic: Boolean) {
        var messageBuilder: Message.Builder = Message.builder()
            .setWebpushConfig(
                WebpushConfig.builder()
                    .setNotification(
                        WebpushNotification.builder()
                            .setTitle(notification.title)
                            .setBody(notification.message)
                            //.setIcon()
                        .build()
                    ).build()
            )
        val message: Message =
            if (isTopic) messageBuilder.setTopic(notification.target).build()
            else messageBuilder.setToken(notification.target).build()
        FirebaseMessaging.getInstance().sendAsync(message)
    }
}
