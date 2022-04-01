package com.duckbox.dto.vote

import java.util.*

data class VoteRegisterDto (
    val title: String,
    val content: String,
    val isGroup: Boolean,
    val owner: String?, // groupId(ObjectId) if group vote else userId(Long)
    val startTime: Date,
    val finishTime: Date,
    var images: List<ByteArray>, // image list
    val candidates: List<String>,
    val voters: List<Int>, // student id
    val reward: Boolean,
    var notice: Boolean
)
