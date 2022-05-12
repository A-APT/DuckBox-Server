package com.duckbox.controller

import BlindSecp256k1
import BlindedData
import com.duckbox.DefinedValue
import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.dto.BlindSigToken
import com.duckbox.service.FCMService
import com.duckbox.service.GroupService
import com.duckbox.service.UserService
import com.duckbox.service.VoteService
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigInteger

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
class VoteControllerTest {

    @LocalServerPort
    private var port: Int = -1

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var voteService: VoteService

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

    private val mockVoteRegisterDto: VoteRegisterDto = MockDto.mockVoteRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun init() {
        baseAddress = "http://localhost:${port}"
        voteRepository.deleteAll()
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        photoRepository.deleteAll()
        groupRepository.deleteAll()
        setFCMService() // set fcmService to mockFcmService
    }

    // Set private fcmService
    private fun setFCMService() {
        VoteService::class.java.getDeclaredField("fcmService").apply {
            isAccessible = true
            set(voteService, mockFcmService)
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
    fun is_registerVote_works_no_headers() {
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/vote", mockVoteRegisterDto, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_registerVote_works_invalid_token() {
        // arrange
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer INVALID_TOKEN"
        }
        val httpEntity = HttpEntity<VoteRegisterDto>(mockVoteRegisterDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_registerVote_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"

        val httpEntity = HttpEntity<VoteRegisterDto>(mockVoteRegisterDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote", HttpMethod.POST, httpEntity, String::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_registerVote_works_well_with_multipart() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"

        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy()
        mockDto.images = listOf("test file!".toByteArray())
        val httpEntity = HttpEntity<VoteRegisterDto>(mockDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote", HttpMethod.POST, httpEntity, String::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_getAllVote_works_well() {
        // arrange
        val token: String = registerAndLogin() // register user
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"
        voteService.registerVote(mockUserEmail, mockVoteRegisterDto) // register vote

        val httpEntity = HttpEntity<String>(null, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote", HttpMethod.GET, httpEntity, Array<VoteDetailDto>::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
                assertThat(body!!.size).isEqualTo(1)
            }
    }

    @Test
    fun is_findVotesOfGroup_works_well() {
        // arrange
        val token: String = registerAndLogin() // register user
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"
        val groupId: String = registerMockGroup() // register group
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(isGroup = true, groupId = groupId)
        voteService.registerVote(mockUserEmail, mockDto) // register vote

        val httpEntity = HttpEntity<String>(groupId, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote/group/$groupId", HttpMethod.GET, httpEntity, Array<VoteDetailDto>::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
                assertThat(body!!.size).isEqualTo(1)
            }
    }

    @Test
    fun is_findVotesOfGroup_works_well_when_unregistered_group() {
        // arrange
        val token: String = registerAndLogin() // register user
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"
        val httpEntity = HttpEntity<String>(null, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote/group/${ObjectId().toString()}", HttpMethod.GET, httpEntity, Array<VoteDetailDto>::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
                assertThat(body!!.size).isEqualTo(0)
            }
    }


    @Test
    fun is_generateVoteToken_works_no_authToken_header() {
        // arrange
        val httpEntity = HttpEntity(ObjectId().toString(), HttpHeaders())

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote/signatures", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_generateVoteToken_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val voteId: String = voteService.registerVote(mockUserEmail, MockDto.mockVoteRegisterDto).body!!

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(DefinedValue.R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = blindedData.blindM.toString(16))
        val httpEntity = HttpEntity(blindSigRequestDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote/signatures", HttpMethod.POST, httpEntity, BlindSigToken::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
                val voteToken: BlindSigToken = body!!
                val serverBSig: BigInteger = BigInteger(voteToken.serverBSig, 16)
                val ownerBSig: BigInteger = BigInteger(voteToken.ownerBSig, 16)
                val serverSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, serverBSig)
                val voteOwnerSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, ownerBSig)

                assertThat(blindSecp256k1.verify(serverSig, blindedData.R, message, DefinedValue.pubkey)).isEqualTo(true)
                assertThat(blindSecp256k1.verify(voteOwnerSig, blindedData.R, message, DefinedValue.ownerPublic)).isEqualTo(true)
            }
    }

    @Test
    fun is_generateVoteToken_works_on_invalid_user() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer invalid_token"
        }
        val voteId = voteService.registerVote(mockUserEmail, MockDto.mockVoteRegisterDto)
        val httpEntity = HttpEntity(voteId.toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote/signatures", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }
}
