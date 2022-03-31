package com.duckbox.dto.vote

import com.duckbox.domain.vote.BallotStatus
import org.bson.types.ObjectId
import java.util.*

data class VoteDetailDto (
    val id: String, // ObjectId
    var title: String,
    var content: String,
    var isGroup: Boolean,
    var groupId: ObjectId?, // required if isGroup is true
    var startTime: Date,
    var finishTime: Date,
    var status: BallotStatus,
    var images: List<ByteArray>, // image list
    var candidates: List<String>,
    var reward: Boolean,
)

