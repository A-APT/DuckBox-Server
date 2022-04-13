package com.duckbox.dto.user

data class BlingSigRequestDto (
    val targetId: String,
    val blindMessage: String, // blindMessage: BigInteger(blindMessage, 16)
)
