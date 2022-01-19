package com.duckbox.dto.user

data class LoginResponseDto (
    val token: String,
    val refreshToken: String
)
