package com.duckbox.dto.notification

data class NotificationMessage(val target: String, val title: String, val message: String)
// target: topic or token
