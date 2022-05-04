package com.duckbox.dto.group

data class JoinGroupRequestDto (
    val email: String,
    val groupId: String // ObjectId
)
