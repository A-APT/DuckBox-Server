package com.duckbox.dto.group

data class ReportRequestDto (
    val groupId: String, // ObjectId
    val reportType: Int,
    val reason: String,
)
