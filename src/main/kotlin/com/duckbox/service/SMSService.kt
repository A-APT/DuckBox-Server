package com.duckbox.service

import com.duckbox.domain.user.SMSAuth
import com.duckbox.domain.user.SMSAuthRepository
import net.nurigo.java_sdk.api.Message
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Service
import kotlin.random.Random

@PropertySource("classpath:security.properties")
@Service
class SMSService (private val smsAuthRepository: SMSAuthRepository) {

    @Value("\${sms.number}")
    private lateinit var fromNumber: String

    @Value("\${sms.api-key}")
    private lateinit var apiKey: String

    @Value("\${sms.api-secret}")
    private lateinit var apiSecret: String

    private val EXPIRATION_TIME: Long = 1000L * 60L * 5L // 5 minute

    fun sendMessage(targetNumber: String, text: String) {
        val coolSMS = Message(apiKey, apiSecret)
        val params = HashMap<String, String>().apply {
            put("to", targetNumber)
            put("from", fromNumber)
            put("type", "SMS")
            put("text", text)
        }
        coolSMS.send(params)
    }

    fun sendSMSAuth(targetNumber: String) {
        val token = createSMSToken(targetNumber)
        val text = "[DuckBox] 본인확인 인증번호: $token"
        sendMessage(targetNumber, text)
    }

    fun createSMSToken(targetNumber: String): String {
        // create token first
        val createTime = System.currentTimeMillis()
        val random = Random(createTime)
        val code = StringBuilder()
        for (i in 0..5) { // validation code: 6 numbers
            code.append(random.nextInt(10))
        }
        val token = code.toString()

        // create EmailAuth entity
        val expirationTime = createTime + EXPIRATION_TIME
        smsAuthRepository.save(
            SMSAuth(
                phoneNumber = targetNumber,
                token = token,
                expirationTime = expirationTime,
                expired = false
            )
        )
        return token
    }

    fun verifySMSToken(targetNumber: String, token: String): Boolean {
        // Find emailAuth
        lateinit var smsAuth: SMSAuth
        runCatching {
            smsAuthRepository.findByPhoneNumber(targetNumber)
        }.onSuccess { smsAuth = it }.onFailure { throw it }

        // Check email token
        val currentTime = System.currentTimeMillis()
        if (smsAuth.token == token && smsAuth.expirationTime >= currentTime) {
            smsAuth.expired = true
            smsAuthRepository.save(smsAuth)
            return true
        } else {
            throw Exception("SMS Authentication Failed: Please check your phone-number or token validation time") // TODO
        }
    }
}