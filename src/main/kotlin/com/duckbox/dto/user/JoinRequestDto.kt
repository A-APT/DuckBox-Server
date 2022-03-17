package com.duckbox.dto.user

import org.bson.types.ObjectId

data class JoinGroupRequestDto (
    val email: String,
    val groupId: ObjectId
)

data class JoinVoteRequestDto (
    val email: String,
    val voteId: ObjectId
)
