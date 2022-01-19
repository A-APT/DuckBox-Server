package com.duckbox.dto

data class JWTToken (
    val token: String,
    val refreshToken: String
)
