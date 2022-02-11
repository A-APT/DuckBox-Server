package com.duckbox.utils

import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class HashUtils {

    fun SHA256(text: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digested = messageDigest.digest(text.toByteArray())
        return digested.joinToString(separator = "") { String.format("%02x", it) }
    }

}
