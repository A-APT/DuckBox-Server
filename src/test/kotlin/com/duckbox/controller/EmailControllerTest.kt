package com.duckbox.controller

import com.duckbox.domain.auth.EmailAuthRepository
import com.duckbox.dto.auth.EmailTokenDto
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.errors.exception.UnauthorizedException
import com.duckbox.errors.exception.UnknownException
import com.duckbox.service.EmailService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class, MockKExtension::class)
class EmailControllerTest {
    @LocalServerPort
    private var port: Int = -1

    @RelaxedMockK
    private lateinit var mockEmailSender: JavaMailSender

    @MockK
    private lateinit var mockEmailService: EmailService

    @Autowired
    private lateinit var emailAuthRepository: EmailAuthRepository

    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    private lateinit var emailController: EmailController

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private lateinit var baseAddress: String

    private val testEmail = "email@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun init() {
        baseAddress = "http://localhost:${port}"
        emailAuthRepository.deleteAll()
        setEmailSender() // set emailSender to mockEmailSender
    }

    // Set private emailSender
    private fun setEmailSender() {
        EmailService::class.java.getDeclaredField("emailSender").apply {
            isAccessible = true
            set(emailService, mockEmailSender)
        }
    }

    // Set emailService of emailController
    private fun setEmailService(emailService: EmailService) {
        EmailController::class.java.getDeclaredField("emailService").apply {
            isAccessible = true
            set(emailController, emailService)
        }
    }

    @Test
    fun is_generateEmailAuth_works_well() {
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/email", testEmail, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }

        val emailAuth = emailAuthRepository.findByEmail(testEmail)
        emailAuth.apply {
            assertThat(email).isEqualTo(testEmail)
            assertThat(expired).isEqualTo(false)
            assertThat(token).isEqualTo(token)
        }
    }

    @Test
    fun is_generateEmailAuth_works_on_MailException() {
        // arrange
        every { mockEmailService.sendEmailAuth(testEmail) } throws UnknownException("Caused by MailException")
        setEmailService(mockEmailService) // set emailService to mockEmailService

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/email", testEmail, UnknownException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        setEmailService(emailService) // set mockEmailService to emailService
    }

    @Test
    fun is_verifyEmailToken_works_well() {
        //  arrange
        emailService.createEmailToken(testEmail)
        val token = emailAuthRepository.findByEmail(testEmail).token
        val emailTokenDto = EmailTokenDto(
            email = testEmail,
            token = token
        )
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/email/verify", emailTokenDto, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
        val emailAuth = emailAuthRepository.findByEmail(testEmail)
        emailAuth.apply {
            assertThat(email).isEqualTo(testEmail)
            assertThat(expired).isEqualTo(true)
        }
    }

    @Test
    fun is_verifyEmailToken_works_on_NOTFOUND() {
        //  arrange
        val emailTokenDto = EmailTokenDto(
            email = testEmail,
            token = "token"
        )
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/email/verify", emailTokenDto, NotFoundException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    fun is_verifyEmailToken_works_on_invalidToken() {
        //  arrange
        emailService.sendEmailAuth(testEmail)
        val emailTokenDto = EmailTokenDto(
            email = testEmail,
            token = "token"
        )
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/email/verify", emailTokenDto, UnauthorizedException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
            }
    }

}
