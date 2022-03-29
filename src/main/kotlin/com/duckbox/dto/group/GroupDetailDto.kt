package com.duckbox.dto.group

import com.duckbox.domain.group.GroupStatus

data class GroupDetailDto (
    val id: String, // ObjectId
    val name: String,
    var leader: String, // did
    var status: GroupStatus,
    var description: String,
    var profile: ByteArray? = null, // image
    var header: ByteArray? = null, // image
)
