package com.duckbox.dto.user

data class BlingSigRequestDto (
    val targetId: String, // vote or survey id (ObjectID)
    val blindMessage: String, // blindMessage: BigInteger(blindMessage, 16)
)
