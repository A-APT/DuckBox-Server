package com.duckbox.controller

import com.duckbox.domain.user.SMSAuthRepository
import com.duckbox.dto.user.SMSTokenDto
import com.duckbox.service.SMSService
import io.mockk.every
import io.mockk.impl.annotations.MockK
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
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class, MockKExtension::class)
class SMSControllerTest {
    @LocalServerPort
    private var port: Int = -1

    @MockK
    private lateinit var mockSMSService: SMSService

    @Autowired
    private lateinit var smsAuthRepository: SMSAuthRepository

    @Autowired
    private lateinit var smsService: SMSService

    @Autowired
    private lateinit var smsController: SMSController

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private lateinit var baseAddress: String

    private val testNumber = "01012341234"

    @BeforeEach
    @AfterEach
    fun init() {
        baseAddress = "http://localhost:${port}"
        smsAuthRepository.deleteAll()
    }

    // Set private emailSender
    private fun setSMSService() {
        SMSController::class.java.getDeclaredField("smsService").apply {
            isAccessible = true
            set(smsController, mockSMSService)
        }
    }

    @Test
    fun is_generateEmailAuth_works_well() {
        // arrange
        //every { mockSMSService.sendMessage(any(), any()) } answers {}
        setSMSService() // set smsService to mockSMSService

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/sms", testNumber, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }

        val smsAuth = smsAuthRepository.findByPhoneNumber(testNumber)
        smsAuth.apply {
            assertThat(phoneNumber).isEqualTo(testNumber)
            assertThat(expired).isEqualTo(false)
            assertThat(token).isEqualTo(token)
        }
    }

    @Test
    fun is_generateEmailAuthAndReturn_works_well() {
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/sms/token", testNumber, String::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
                val smsAuth = smsAuthRepository.findByPhoneNumber(testNumber)
                smsAuth.apply {
                    assertThat(phoneNumber).isEqualTo(testNumber)
                    assertThat(expired).isEqualTo(false)
                    assertThat(token).isEqualTo(body!!)
                }
            }
    }

    @Test
    fun is_verifyEmailToken_works_well() {
        //  arrange
        smsService.createSMSToken(testNumber)
        val token = smsAuthRepository.findByPhoneNumber(testNumber).token
        val smsTokenDto = SMSTokenDto(
            phoneNumber = testNumber,
            token = token
        )

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/sms/verify", smsTokenDto, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
        val smsAuth = smsAuthRepository.findByPhoneNumber(testNumber)
        smsAuth.apply {
            assertThat(phoneNumber).isEqualTo(testNumber)
            assertThat(expired).isEqualTo(true)
        }
    }

    // TODO error handling
}
