package com.duckbox.controller

import com.duckbox.dto.auth.SMSTokenDto
import com.duckbox.service.SMSService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SMSController (private val smsService: SMSService) {

    @PostMapping("/api/v1/user/sms")
    fun generateSMSAuth(@RequestBody targetNumber: String): ResponseEntity<Unit> {
        smsService.sendSMSAuth(targetNumber)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/v1/user/sms/token")
    fun generateSMSAuthAndReturn(@RequestBody targetNumber: String): ResponseEntity<String> {
        val token = smsService.createSMSToken(targetNumber)
        return ResponseEntity.status(HttpStatus.OK).body(token)
    }

    @PostMapping("/api/v1/user/sms/verify")
    fun verifySMSToken(@RequestBody smsTokenDto: SMSTokenDto): ResponseEntity<Unit> {
        smsService.verifySMSToken(smsTokenDto.phoneNumber, smsTokenDto.token)
        return ResponseEntity.noContent().build()
    }

}
