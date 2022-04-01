package com.duckbox.controller

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.JWTToken
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.user.*
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.errors.exception.UnauthorizedException
import com.duckbox.service.GroupService
import com.duckbox.service.UserService
import com.duckbox.service.VoteService
import org.assertj.core.api.Assertions
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
class UserControllerTest {
    @LocalServerPort
    private var port: Int = -1

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userBoxRepository: UserBoxRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private lateinit var baseAddress: String

    private val mockRegisterDto: RegisterDto = MockDto.mockRegisterDto

    @BeforeEach
    @AfterEach
    fun initTest() {
        baseAddress = "http://localhost:${port}"
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        groupRepository.deleteAll()
        voteRepository.deleteAll()
    }

    fun registerAndLogin(): String {
        userService.register(mockRegisterDto)
        return userService
            .login(LoginRequestDto(mockRegisterDto.email, mockRegisterDto.password))
            .body!!.token
    }

    @Test
    fun is_register_works_well() {
        // arrange
        val registerDto = mockRegisterDto

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/register", registerDto, String::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
                assertThat(body!!).isEqualTo(userRepository.findByEmail(mockRegisterDto.email).did)
            }
    }

    @Test
    fun is_register_works_on_duplicate() {
        // arrange
        userService.register(mockRegisterDto)

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/register", mockRegisterDto, String::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.CONFLICT)
            }
    }

    @Test
    fun is_login_works_well() {
        // arrange
        userService.register(mockRegisterDto)
        val loginRequestDto = LoginRequestDto(
            email = mockRegisterDto.email,
            password = mockRegisterDto.password
        )

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/login", loginRequestDto, LoginResponseDto::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_login_works_on_invalidPW() {
        // arrange
        userService.register(mockRegisterDto)
        val loginRequestDto = LoginRequestDto(
            email = mockRegisterDto.email,
            password = "invalid" // invalid password
        )

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/login", loginRequestDto, NotFoundException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    fun is_login_works_on_NOTFOUND_user() {
        // not register any user
        // arrange
        val loginRequestDto = LoginRequestDto(
            email = mockRegisterDto.email,
            password = "invalid"
        )

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/login", loginRequestDto, NotFoundException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    fun is_refreshToken_works_well() {
        // arrange
        userService.register(mockRegisterDto)
        val loginRequestDto = LoginRequestDto(
            email = mockRegisterDto.email,
            password = mockRegisterDto.password
        )
        val loginResponseDto = userService.login(loginRequestDto)

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/refresh", loginResponseDto.body!!.refreshToken, JWTToken::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_refreshToken_works_on_invalidToken() {
        // arrange
        userService.register(mockRegisterDto)
        val loginRequestDto = LoginRequestDto(
            email = mockRegisterDto.email,
            password = mockRegisterDto.password
        )
        userService.login(loginRequestDto)

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/refresh", "invalid-token", UnauthorizedException::class.java)
            .body!!
            .apply {
                assertThat(message).isEqualTo("Failed when refresh token.")
            }
    }

    @Test
    fun is_joinGroup_works_no_authToken_header() {
        // arrange
        val httpEntity = HttpEntity(ObjectId().toString(), HttpHeaders())

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/group", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_joinGroup_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockRegisterDto.email).did)
        val groupId = groupService.registerGroup(mockRegisterDto.email, mockDto)
        val httpEntity = HttpEntity(groupId.toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/group", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
    }

    @Test
    fun is_joinGroup_works_on_invalid_user() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer invalid_token"
        }
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockRegisterDto.email).did)
        val groupId = groupService.registerGroup(mockRegisterDto.email, mockDto)
        val httpEntity = HttpEntity(groupId.toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/group", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_joinGroup_works_on_invalid_groupId() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val httpEntity = HttpEntity(ObjectId().toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/group", HttpMethod.POST, httpEntity, NotFoundException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    fun is_joinVote_works_no_authToken_header() {
        // arrange
        val httpEntity = HttpEntity(ObjectId().toString(), HttpHeaders())

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/vote", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_joinVote_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val voteId = voteService.registerVote(mockRegisterDto.email, MockDto.mockVoteRegisterDto).toString()
        val httpEntity = HttpEntity(voteId, httpHeaders)
        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/vote", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
    }

    @Test
    fun is_joinVote_works_on_invalid_user() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer invalid_token"
        }
        val voteId = voteService.registerVote(mockRegisterDto.email, MockDto.mockVoteRegisterDto)
        val httpEntity = HttpEntity(voteId.toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/vote", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_joinVote_works_on_invalid_groupId() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val invalidVoteId = ObjectId()
        val httpEntity = HttpEntity(invalidVoteId.toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/vote", HttpMethod.POST, httpEntity, NotFoundException::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    fun is_findGroupsByUser_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockRegisterDto.email).did)
        val groupId = groupService.registerGroup(mockRegisterDto.email, mockDto)
        userService.joinGroup(mockRegisterDto.email, groupId.toString())

        val httpEntity = HttpEntity(null, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/user/group", HttpMethod.GET, httpEntity, Array<GroupDetailDto>::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
                assertThat(body!!.size).isEqualTo(1)
            }
    }
}
