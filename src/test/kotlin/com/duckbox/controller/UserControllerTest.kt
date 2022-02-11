package com.duckbox.controller

import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.JWTToken
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.LoginResponseDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.errors.exception.UnauthorizedException
import com.duckbox.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
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
@ExtendWith(SpringExtension::class)
class UserControllerTest {
    @LocalServerPort
    private var port: Int = -1

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private lateinit var baseAddress: String

    @BeforeEach
    @AfterEach
    fun initTest() {
        baseAddress = "http://localhost:${port}"
        userRepository.deleteAll()
    }

    val mockRegisterDto = RegisterDto(
        studentId = 2019333,
        name = "je",
        password = "test",
        email = "email@konkuk.ac.kr",
        phoneNumber = "01012341234",
        nickname = "duck",
        college = "ku",
        department = "computer"
    )

    @Test
    fun is_register_works_well() {
        // arrange
        val registerDto = mockRegisterDto

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/register", registerDto, Unit::class.java)
            .apply {
                assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
    }

    @Test
    fun is_register_works_on_duplicate() {
        // arrange
        userService.register(mockRegisterDto)

        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/user/register", mockRegisterDto, Unit::class.java)
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
            .body
            .apply {
                assertThat(this).isNotEqualTo(null)
                assertThat(token).isNotEqualTo(null)
                assertThat(refreshToken).isNotEqualTo(null)
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
            .body
            .apply {
                assertThat(this).isNotEqualTo(null)
                assertThat(token).isNotEqualTo(null)
                assertThat(refreshToken).isNotEqualTo(null)
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
}
