package com.duckbox.controller

import com.duckbox.dto.user.EmailTokenDto
import com.duckbox.service.EmailService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class EmailController (private val emailService: EmailService) {

    @PostMapping("/api/v1/user/email")
    fun emailAuth(@RequestBody targetEmail: String): ResponseEntity<Unit> {
        emailService.sendEmailAuth(targetEmail)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/v1/user/email/verify")
    fun verifyEmailToken(@RequestBody emailTokenDto: EmailTokenDto): ResponseEntity<Unit> {
        emailService.verifyEmailToken(emailTokenDto.email, emailTokenDto.token)
        return ResponseEntity.noContent().build()
    }
}
