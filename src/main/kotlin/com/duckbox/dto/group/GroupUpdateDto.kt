package com.duckbox.dto.group

data class GroupUpdateDto (
    var id: String, // ObjectId
    //var leader: String, // did
    var description: String?,
    var profile: ByteArray? = null, // image
    var header: ByteArray? = null, // image
)
