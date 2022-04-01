package com.duckbox.dto.vote

import com.duckbox.domain.vote.BallotStatus
import java.util.*

data class VoteDetailDto (
    val id: String, // ObjectId
    var title: String,
    var content: String,
    var isGroup: Boolean,
    var owner: String, // groupId(ObjectId) if group vote else userId(Long)
    var startTime: Date,
    var finishTime: Date,
    var status: BallotStatus,
    var images: List<ByteArray>, // image list
    var candidates: List<String>,
    var reward: Boolean,
)

