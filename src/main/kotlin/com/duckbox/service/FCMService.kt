package com.duckbox.service

import com.duckbox.dto.notification.NotificationMessage
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.springframework.stereotype.Service

@Service
class FCMService {
    fun sendNotification(notification: NotificationMessage, isTopic: Boolean) {
        var messageBuilder: Message.Builder = Message.builder()
            .putData("id", notification.id)
            .putData("title", notification.title)
            .putData("type", notification.type.toString())
        val message: Message =
            if (isTopic) messageBuilder.setTopic(notification.target).build()
            else messageBuilder.setToken(notification.target).build()
        FirebaseMessaging.getInstance().sendAsync(message)
    }
}
