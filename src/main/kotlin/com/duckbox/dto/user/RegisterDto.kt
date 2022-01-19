package com.duckbox.dto.user

data class RegisterDto (
    val studentId: Int,
    val name: String,
    val password: String,
    val email: String,
    val nickname: String,
    val college: String,
    val department: String
)