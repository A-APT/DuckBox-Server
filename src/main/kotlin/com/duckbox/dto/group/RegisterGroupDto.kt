package com.duckbox.dto.group

import org.springframework.web.multipart.MultipartFile

class RegisterGroupDto (
    val name: String,
    var leader: String, // did
    var description: String,
    var profile: MultipartFile? = null, // image
    var header: MultipartFile? = null, // image
)
