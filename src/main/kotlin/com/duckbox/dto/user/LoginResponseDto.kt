package com.duckbox.dto.user

data class LoginResponseDto (
    val token: String,
    val refreshToken: String,
    val did: String,
    val studentId: Int,
)
