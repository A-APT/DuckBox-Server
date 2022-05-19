package com.duckbox.controller

import BlindSecp256k1
import BlindedData
import com.duckbox.DefinedValue
import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.survey.SurveyRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.BlindSigToken
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.dto.survey.SurveyDetailDto
import com.duckbox.dto.survey.SurveyRegisterDto
import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.service.FCMService
import com.duckbox.service.GroupService
import com.duckbox.service.SurveyService
import com.duckbox.service.UserService
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigInteger

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
class SurveyControllerTest {

    @LocalServerPort
    private var port: Int = -1

    @Autowired
    private lateinit var surveyRepository: SurveyRepository

    @Autowired
    private lateinit var surveyService: SurveyService

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userBoxRepository: UserBoxRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var blindSecp256k1: BlindSecp256k1

    private lateinit var baseAddress: String

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private var mockFcmService: FCMService = mockk(relaxed = true)

    private val mockSurveyRegisterDto: SurveyRegisterDto = MockDto.mockSurveyRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun init() {
        baseAddress = "http://localhost:${port}"
        surveyRepository.deleteAll()
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        photoRepository.deleteAll()
        groupRepository.deleteAll()
        setFCMService() // set fcmService to mockFcmService
    }

    // Set private fcmService
    private fun setFCMService() {
        SurveyService::class.java.getDeclaredField("fcmService").apply {
            isAccessible = true
            set(surveyService, mockFcmService)
        }
    }

    fun registerAndLogin(): String {
        userService.register(
            RegisterDto(
                studentId = 2019333,
                name = "je",
                password = "test",
                email = mockUserEmail,
                phoneNumber = "01012341234",
                nickname = "duck",
                college = "ku",
                department = listOf("computer", "software"),
                fcmToken = "temp",
            )
        )
        return userService.login(
            LoginRequestDto(email = mockUserEmail, password = "test")
        ).body!!.token
    }

    fun registerMockGroup(): String {
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        return groupService.registerGroup(mockUserEmail, mockDto).body!!
    }

    @Test
    fun is_registerSurvey_works_no_headers() {
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/survey", mockSurveyRegisterDto, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_registerSurvey_works_invalid_token() {
        // arrange
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer INVALID_TOKEN"
        }
        val httpEntity = HttpEntity<SurveyRegisterDto>(mockSurveyRegisterDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_registerSurvey_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"

        val httpEntity = HttpEntity<SurveyRegisterDto>(mockSurveyRegisterDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey", HttpMethod.POST, httpEntity, String::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_registerSurvey_works_well_with_multipart() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"

        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy()
        mockDto.images = listOf("test file!".toByteArray())
        val httpEntity = HttpEntity<SurveyRegisterDto>(mockDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey", HttpMethod.POST, httpEntity, String::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_getAllSurvey_works_well() {
        // arrange
        val token: String = registerAndLogin() // register user
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"
        surveyService.registerSurvey(mockUserEmail, mockSurveyRegisterDto) // register survey

        val httpEntity = HttpEntity<String>(null, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey", HttpMethod.GET, httpEntity, Array<SurveyDetailDto>::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
                Assertions.assertThat(body!!.size).isEqualTo(1)
            }
    }

    @Test
    fun is_findSurveysOfGroup_works_no_headers() {
        // act, assert
        restTemplate
            .getForEntity("${baseAddress}/api/v1/survey/group/${ObjectId().toString()}", String::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_findSurveysOfGroup_works_well() {
        // arrange
        val token: String = registerAndLogin() // register user
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"
        val groupId: String = registerMockGroup() // register group
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId)
        surveyService.registerSurvey(mockUserEmail, mockDto) // register survey

        val httpEntity = HttpEntity(null, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey/group/$groupId", HttpMethod.GET, httpEntity, Array<SurveyDetailDto>::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
                Assertions.assertThat(body!!.size).isEqualTo(1)
            }
    }

    @Test
    fun is_findSurveysOfGroup_works_well_when_unregistered_group() {
        // arrange
        val token: String = registerAndLogin() // register user
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"
        val httpEntity = HttpEntity<String>(null, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey/group/${ObjectId().toString()}", HttpMethod.GET, httpEntity, Array<SurveyDetailDto>::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
                Assertions.assertThat(body!!.size).isEqualTo(0)
            }
    }
    
    @Test
    fun is_generateSurveyToken_works_no_authToken_header() {
        // arrange
        val httpEntity = HttpEntity(ObjectId().toString(), HttpHeaders())

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey/signatures", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_generateSurveyToken_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, MockDto.mockSurveyRegisterDto).body!!

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(DefinedValue.R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = surveyId, blindMessage = blindedData.blindM.toString(16))
        val httpEntity = HttpEntity(blindSigRequestDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey/signatures", HttpMethod.POST, httpEntity, BlindSigToken::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
                val surveyToken: BlindSigToken = body!!
                val serverBSig: BigInteger = BigInteger(surveyToken.serverBSig, 16)
                val ownerBSig: BigInteger = BigInteger(surveyToken.ownerBSig, 16)
                val serverSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, serverBSig)
                val surveyOwnerSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, ownerBSig)

                Assertions.assertThat(blindSecp256k1.verify(serverSig, blindedData.R, message, DefinedValue.pubkey)).isEqualTo(true)
                Assertions.assertThat(
                    blindSecp256k1.verify(
                        surveyOwnerSig,
                        blindedData.R,
                        message,
                        DefinedValue.ownerPublic
                    )
                ).isEqualTo(true)
            }
    }

    @Test
    fun is_generateSurveyToken_works_on_invalid_user() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer invalid_token"
        }
        val surveyId = surveyService.registerSurvey(mockUserEmail, MockDto.mockSurveyRegisterDto)
        val httpEntity = HttpEntity(surveyId.toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/survey/signatures", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }
}