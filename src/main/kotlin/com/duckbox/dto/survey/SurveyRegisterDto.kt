package com.duckbox.dto.survey

import com.duckbox.domain.survey.Question
import java.util.*

data class SurveyRegisterDto (
    val title: String,
    val content: String,
    val isGroup: Boolean,
    var groupId: String?, // groupId(ObjectId) if isGroup is true
    val startTime: Date,
    val finishTime: Date,
    var images: List<ByteArray>, // image list
    var ownerPrivate: String, // private key in radix 16
    var questions: List<Question>,
    var targets: List<Int>?, // student id. null if isGroup is false or all group member have right to survey
    val reward: Boolean,
    var notice: Boolean
)