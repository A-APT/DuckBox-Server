package com.duckbox.dto

import com.duckbox.domain.survey.Question
import com.duckbox.domain.vote.BallotStatus
import java.util.*

/* information of group vote or survey */
data class OverallDetailDto (
    val id: String, // ObjectId
    var title: String,
    var content: String,
    var groupId: String, // ObjectId
    var owner: String, // owner's nickname
    var startTime: Date,
    var finishTime: Date,
    var status: BallotStatus,
    var images: List<ByteArray>, // image list
    var questions: List<Question>, // if isVote is true, size of questions is 1
    var targets: List<Int>?, // student id
    var reward: Boolean,
    var isVote: Boolean,
    var isAvailable: Boolean, // true if user have not participated to this vote/survey
)
