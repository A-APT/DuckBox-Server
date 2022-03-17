package com.duckbox.controller

import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.service.UserService
import org.assertj.core.api.Assertions
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
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
class VoteControllerTest {

    @LocalServerPort
    private var port: Int = -1

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    private lateinit var baseAddress: String

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mockVoteRegisterDto = VoteRegisterDto(
        title = "title",
        content = "content",
        isGroup = false,
        groupId = null,
        startTime = Date(),
        finishTime = Date(),
        images = listOf(),
        candidates = listOf("a", "b"),
        voters = listOf(1, 2),
        reward = false,
        notice = false
    )

    @BeforeEach
    @AfterEach
    fun init() {
        baseAddress = "http://localhost:${port}"
        voteRepository.deleteAll()
        userRepository.deleteAll()
        photoRepository.deleteAll()
    }

    fun registerAndLogin(): String {
        userService.register(
            RegisterDto(
                studentId = 2019333,
                name = "je",
                password = "test",
                email = "email@konkuk.ac.kr",
                phoneNumber = "01012341234",
                nickname = "duck",
                college = "ku",
                department = listOf("computer", "software")
            )
        )
        return userService.login(
            LoginRequestDto(email = "email@konkuk.ac.kr", password = "test")
        ).body!!.token
    }

    @Test
    fun is_registerVote_works_no_headers() {
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/vote/register", mockVoteRegisterDto, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
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
            .exchange("${baseAddress}/api/v1/vote/register", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
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
            .exchange("${baseAddress}/api/v1/vote/register", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
    }

    @Test
    fun is_registerVote_works_well_with_multipart() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders()
        httpHeaders["Authorization"] = "Bearer $token"

        mockVoteRegisterDto.images = listOf("test file!".toByteArray())
        val httpEntity = HttpEntity<VoteRegisterDto>(mockVoteRegisterDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/vote/register", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
    }
}
