package com.duckbox.service

import com.duckbox.domain.user.EmailAuthRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
class EmailServiceTest {

    @Autowired
    private lateinit var emailAuthRepository: EmailAuthRepository

    @Autowired
    private lateinit var emailService: EmailService

    private val testEmail = "email@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun init() {
        emailAuthRepository.deleteAll()
    }

    @Test
    fun is_createEmailToken_works_well() {
        // act
        val token = emailService.createEmailToken(testEmail)

        // assert
        assertThat(token.length).isEqualTo(6)
        val emailAuth = emailAuthRepository.findByEmail(testEmail)
        emailAuth.apply {
            assertThat(email).isEqualTo(testEmail)
            assertThat(expired).isEqualTo(false)
            assertThat(token).isEqualTo(token)
        }
    }

    @Test
    fun is_verifyEmailToken_works_well() {
        // arrange
        val token = emailService.createEmailToken(testEmail)

        // act
        val result = emailService.verifyEmailToken(testEmail, token)

        // assert
        assertThat(result).isEqualTo(true)
        val emailAuth = emailAuthRepository.findByEmail(testEmail)
        emailAuth.apply {
            assertThat(email).isEqualTo(testEmail)
            assertThat(expired).isEqualTo(true)
        }
    }

    @Test
    fun is_verifyEmailToken_works_when_invalid_email() {
        // act, assert
        runCatching {
            emailService.verifyEmailToken(testEmail, "token")
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            // TODO
        }
    }

    @Test
    fun is_verifyEmailToken_works_when_invalid_token() {
        // arrange
        emailService.createEmailToken(testEmail)

        // act, assert
        runCatching {
            emailService.verifyEmailToken(testEmail, "invalid-token")
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it.message).isEqualTo("Email Authentication Failed: Please check your email or token validation time")
        }
    }

    @Test
    fun is_verifyEmailToken_works_when_token_expired() {
        // arrange
        emailService.createEmailToken(testEmail)
        val emailAuth = emailAuthRepository.findByEmail(testEmail)
        emailAuth.expirationTime = System.currentTimeMillis() // change expirationTime for testing
        emailAuthRepository.save(emailAuth)

        // act, assert
        runCatching {
            emailService.verifyEmailToken(testEmail, "invalid-token")
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it.message).isEqualTo("Email Authentication Failed: Please check your email or token validation time")
        }
    }
}
