package com.duckbox.dto.notification

data class NotificationMessage(
    val target: String, // target: topic or token
    val id: String, // group or vote or survey Id
    val title: String, // group name
    val type: Int, // group(0), vote(1), survey(2)
)
