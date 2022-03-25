package com.duckbox.dto.user

data class JoinGroupRequestDto (
    val email: String,
    val groupId: String // ObjectId
)

data class JoinVoteRequestDto (
    val email: String,
    val voteId: String // ObjectId
)
