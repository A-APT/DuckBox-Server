package com.duckbox.controller

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.service.GroupService
import com.duckbox.service.UserService
import com.duckbox.service.VoteService
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
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    private lateinit var baseAddress: String

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mockVoteRegisterDto: VoteRegisterDto = MockDto.mockVoteRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun init() {
        baseAddress = "http://localhost:${port}"
        voteRepository.deleteAll()
        userRepository.deleteAll()
        photoRepository.deleteAll()
        groupRepository.deleteAll()
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
                department = listOf("computer", "software")
            )
        )
        return userService.login(
            LoginRequestDto(email = mockUserEmail, password = "test")
        ).body!!.token
    }

    fun registerMockGroup(): String {
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        return groupService.registerGroup(mockUserEmail, mockDto).toString()
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
            .exchange("${baseAddress}/api/v1/vote", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
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
            .exchange("${baseAddress}/api/v1/vote", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
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
}
