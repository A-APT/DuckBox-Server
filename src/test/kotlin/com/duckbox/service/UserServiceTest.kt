package com.duckbox.service

import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.LoginResponseDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.security.JWTTokenProvider
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
class UserServiceTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    @BeforeEach
    @AfterEach
    fun init() {
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
        // act
        userService.register(mockRegisterDto)

        // assert
        val user: User = userRepository.findByEmail(mockRegisterDto.email)
        assertThat(user.email).isEqualTo(mockRegisterDto.email)
        assertThat(user.password).isEqualTo(mockRegisterDto.password)
    }

    @Test
    fun is_login_works_well() {
        // arrange
        userService.register(mockRegisterDto)
        val loginRequestDto = LoginRequestDto(
            email = mockRegisterDto.email,
            password = mockRegisterDto.password
        )

        // act
        val loginResponseDto: LoginResponseDto? = userService.login(loginRequestDto).body

        // assert
        val user: User = userRepository.findByEmail(loginRequestDto.email)
        assertThat(user.email).isEqualTo(loginRequestDto.email)
        assertThat(user.password).isEqualTo(loginRequestDto.password)
        assertThat(loginResponseDto).isNotEqualTo(null)
        loginResponseDto?.apply {
            assertThat(jwtTokenProvider.verifyToken(token)).isEqualTo(true)
            assertThat(jwtTokenProvider.getUserPK(token)).isEqualTo(loginRequestDto.email)
        }
    }

    @Test
    fun is_login_works_well_when_invalidPW() {
        // arrange
        userService.register(mockRegisterDto)
        val loginRequestDto = LoginRequestDto(
            email = mockRegisterDto.email,
            password = "invalid"
        )

        // act, assert
        runCatching {
            userService.login(loginRequestDto).body
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            // TODO
        }
    }
}
