package com.duckbox.dto.vote

import java.util.*

data class VoteRegisterDto (
    val title: String,
    val content: String,
    val isGroup: Boolean,
    val groupId: String?, // ObjectId // required if isGroup is true
    val startTime: Date,
    val finishTime: Date,
    var images: List<ByteArray>, // image list
    val candidates: List<String>,
    val voters: List<Int>, // student id
    val reward: Boolean,
    var notice: Boolean
)
