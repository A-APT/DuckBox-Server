package com.duckbox.controller

import com.duckbox.domain.auth.SMSAuthRepository
import com.duckbox.dto.auth.SMSTokenDto
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.errors.exception.UnauthorizedException
import com.duckbox.errors.exception.UnknownException
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

    // Set SMSService of smsController
    private fun setSMSService(smsService: SMSService) {
        SMSController::class.java.getDeclaredField("smsService").apply {
            isAccessible = true
            set(smsController, smsService)
        }
    }

    @Test
    fun is_generateEmailAuth_works_well() { // TODO check any() works well...
        // arrange
        every { mockSMSService.sendMessage(any(), any()) } answers {}
        every { mockSMSService.sendSMSAuth(any())} answers {
            smsService.createSMSToken(testNumber)
            mockSMSService.sendMessage(testNumber, "txt")
        }
        setSMSService(mockSMSService) // set smsService to mockSMSService

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
        setSMSService(smsService) // set mockSMSService to smsService
    }

    @Test
    fun is_generateEmailAuth_works_sendMessage_error() { // TODO check any() works well...
        // arrange
        every { mockSMSService.sendMessage(any(), any()) } throws UnknownException("Caused by CoolsmsException")
        every { mockSMSService.sendSMSAuth(any())} answers {
            smsService.createSMSToken(testNumber)
            mockSMSService.sendMessage(testNumber, "txt")
        }
        setSMSService(mockSMSService) // set smsService to mockSMSService

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/sms", testNumber, UnknownException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        setSMSService(smsService) // set mockSMSService to smsService
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
    fun is_verifySMSToken_works_well() {
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

    @Test
    fun is_verifyEmailToken_works_on_NOTFOUND() {
        //  arrange
        val smsTokenDto = SMSTokenDto(
            phoneNumber = testNumber,
            token = "token"
        )

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/sms/verify", smsTokenDto, NotFoundException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    fun is_verifyEmailToken_works_on_invalidToken() {
        //  arrange
        smsService.createSMSToken(testNumber)
        val smsTokenDto = SMSTokenDto(
            phoneNumber = testNumber,
            token = "token"
        )

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/sms/verify", smsTokenDto, UnauthorizedException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
            }
    }
}
