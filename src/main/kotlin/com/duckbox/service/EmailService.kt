package com.duckbox.service

import com.duckbox.domain.user.EmailAuth
import com.duckbox.domain.user.EmailAuthRepository
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.errors.exception.UnauthorizedException
import com.duckbox.errors.exception.UnknownException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class EmailService (
    private val emailAuthRepository: EmailAuthRepository,
    private val emailSender: JavaMailSender)
{
    private val EXPIRATION_TIME: Long = 1000L * 60L * 10L // 10 minute

    private fun sendEmail(targetEmail: String, subject: String, text: String) {
        val message = SimpleMailMessage()

        message.setTo(targetEmail)
        message.subject = subject
        message.text = text

        runCatching {
            emailSender.send(message)
        }.onFailure {
            throw UnknownException("Caused by MailException")
        }
    }

    fun sendEmailAuth(targetEmail: String) {
        val token = createEmailToken(targetEmail)
        val subject = "DuckBox Email Authentication"
        val text = "Verify your email $targetEmail with this 6-numbers-code: $token"
        sendEmail(targetEmail, subject, text)
    }

    fun createEmailToken(targetEmail: String): String {
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
        emailAuthRepository.save(EmailAuth(
            email = targetEmail,
            token = token,
            expirationTime = expirationTime,
            expired = false
        ))

        return token
    }

    fun verifyEmailToken(targetEmail: String, token: String): Boolean {
        // Find emailAuth
        lateinit var emailAuth: EmailAuth
        runCatching {
            emailAuthRepository.findByEmail(targetEmail)
        }.onSuccess {
            emailAuth = it
        }.onFailure {
            throw NotFoundException("No authentication token for [${targetEmail}]")
        }

        // Check email token
        val currentTime = System.currentTimeMillis()
        if (emailAuth.token == token && emailAuth.expirationTime >= currentTime) {
            emailAuth.expired = true
            emailAuthRepository.save(emailAuth)
            return true
        } else {
            throw UnauthorizedException("Email Authentication Failed: Please check your email or token validation time")
        }
    }
}
