package com.duckbox.dto.vote

import com.duckbox.domain.vote.BallotStatus
import java.util.*

data class VoteDetailDto (
    val id: String, // ObjectId
    var title: String,
    var content: String,
    var isGroup: Boolean,
    var groupId: String?, // groupId(ObjectId) if isGroup is true
    var owner: String, // owner's nickname
    var startTime: Date,
    var finishTime: Date,
    var status: BallotStatus,
    var images: List<ByteArray>, // image list
    var candidates: List<String>,
    val voters: List<Int>?, // student id. null if isGroup is false or all group member have right to vote
    var reward: Boolean,
)

