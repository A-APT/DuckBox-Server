package com.duckbox.service

import com.duckbox.domain.user.SMSAuthRepository
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
class SMSServiceTest {

    @Autowired
    private lateinit var smsAuthRepository: SMSAuthRepository

    @Autowired
    private lateinit var smsService: SMSService

    private val testNumber = "01012341234" // cleanUP

    @BeforeEach
    @AfterEach
    fun init() {
        smsAuthRepository.deleteAll()
    }

    @Test
    fun is_createSMSToken_works_well() {
        // act
        val token = smsService.createSMSToken(testNumber)

        // assert
        assertThat(token.length).isEqualTo(6)
        val smsAuth = smsAuthRepository.findByPhoneNumber(testNumber)
        smsAuth.apply {
            assertThat(phoneNumber).isEqualTo(testNumber)
            assertThat(expired).isEqualTo(false)
            assertThat(token).isEqualTo(token)
        }
    }

    @Test
    fun is_verifySMSToken_works_well() {
        // arrange
        val token = smsService.createSMSToken(testNumber)

        // act
        val result = smsService.verifySMSToken(testNumber, token)

        // assert
        assertThat(result).isEqualTo(true)
        val smsAuth = smsAuthRepository.findByPhoneNumber(testNumber)
        smsAuth.apply {
            assertThat(phoneNumber).isEqualTo(testNumber)
            assertThat(expired).isEqualTo(true)
        }
    }

    @Test
    fun is_verifySMSToken_works_when_invalid_email() {
        // act, assert
        runCatching {
            smsService.verifySMSToken(testNumber, "token")
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            // TODO
        }
    }

    @Test
    fun is_verifySMSToken_works_when_invalid_token() {
        // arrange
        smsService.createSMSToken(testNumber)

        // act, assert
        runCatching {
            smsService.verifySMSToken(testNumber, "invalid-token")
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it.message).isEqualTo("SMS Authentication Failed: Please check your phone-number or token validation time")
        }
    }

    @Test
    fun is_verifySMSToken_works_when_token_expired() {
        // arrange
        smsService.createSMSToken(testNumber)
        val smsAuth = smsAuthRepository.findByPhoneNumber(testNumber)
        smsAuth.expirationTime = System.currentTimeMillis() // change expirationTime for testing
        smsAuthRepository.save(smsAuth)

        // act, assert
        runCatching {
            smsService.verifySMSToken(testNumber, "invalid-token")
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it.message).isEqualTo("SMS Authentication Failed: Please check your phone-number or token validation time")
        }
    }
}
