package com.duckbox.dto.survey

import com.duckbox.domain.survey.Question
import com.duckbox.domain.vote.BallotStatus
import java.util.*

data class SurveyDetailDto (
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
    var questions: List<Question>,
    var targets: List<Int>?, // student id. null if isGroup is false or all group member have right to survey
    var reward: Boolean,
)