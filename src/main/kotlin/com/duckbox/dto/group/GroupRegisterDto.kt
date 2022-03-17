package com.duckbox.dto.group

data class GroupRegisterDto (
    val name: String,
    var leader: String, // did
    var description: String,
    var profile: ByteArray? = null, // image
    var header: ByteArray? = null, // image
)
