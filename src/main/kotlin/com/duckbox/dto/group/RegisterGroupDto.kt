package com.duckbox.dto.group

class RegisterGroupDto (
    val name: String,
    var leader: String, // did
    var description: String,
    var profile: String? = null, // image
    var header: String? = null, // image
)
