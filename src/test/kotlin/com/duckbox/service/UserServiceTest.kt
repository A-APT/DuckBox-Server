package com.duckbox.service

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserBox
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.JWTToken
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.LoginResponseDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.errors.exception.UnauthorizedException
import com.duckbox.security.JWTTokenProvider
import com.duckbox.service.ethereum.DIdService
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.bson.types.ObjectId
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
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userBoxRepository: UserBoxRepository

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    private lateinit var mockDidService: DIdService

    private val mockRegisterDto: RegisterDto = MockDto.mockRegisterDto

    @BeforeEach
    @AfterEach
    fun init() {
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        groupRepository.deleteAll()
        mockkConstructor(DIdService::class)
        mockDidService = mockk(relaxed = true)
        setDidService(mockDidService)
    }

    // Set ballotService
    private fun setDidService(didService: DIdService) {
        UserService::class.java.getDeclaredField("didService").apply {
            isAccessible = true
            set(userService, didService)
        }
    }

    @Test
    fun is_register_works_well() {
        // act
        val did: String = userService.register(mockRegisterDto).body!!

        // assert
        val user: User = userRepository.findByEmail(mockRegisterDto.email)
        assertThat(user.email).isEqualTo(mockRegisterDto.email)
        assertThat(user.password).isEqualTo(mockRegisterDto.password)
        assertThat(user.did).isEqualTo(did)

        val userBox: UserBox = userBoxRepository.findByEmail(mockRegisterDto.email)
        assertThat(userBox.id).isEqualTo(user.id)
        assertThat(userBox.email).isEqualTo(mockRegisterDto.email)
        assertThat(userBox.groups.size).isEqualTo(0)
        assertThat(userBox.votes.size).isEqualTo(0)
    }

    @Test
    fun is_register_works_on_duplicate_email() {
        // act
        userService.register(mockRegisterDto)
        runCatching {
            userService.register(mockRegisterDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ConflictException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User email [${mockRegisterDto.email}] is already registered.")
        }
    }

    @Test
    fun is_register_works_on_duplicate_nickname() {
        // act
        userService.register(mockRegisterDto)
        val mockDto = mockRegisterDto.copy(email = "new@com")
        runCatching {
            userService.register(mockDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ConflictException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User nickname [${mockDto.nickname}] is already registered.")
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

        // act
        val loginResponseDto: LoginResponseDto? = userService.login(loginRequestDto).body

        // assert
        val user: User = userRepository.findByEmail(loginRequestDto.email)
        assertThat(user.email).isEqualTo(loginRequestDto.email)
        assertThat(user.password).isEqualTo(loginRequestDto.password)
        assertThat(loginResponseDto).isNotEqualTo(null)
        assertThat(loginResponseDto!!.did).isEqualTo(user.did)
        assertThat(loginResponseDto.studentId).isEqualTo(user.studentId)
        assertThat(loginResponseDto.nickname).isEqualTo(user.nickname)
        loginResponseDto.apply {
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
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User email or password was wrong.")
        }
    }

    @Test
    fun is_login_works_well_when_NotFoundUser() {
        // arrange
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
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${loginRequestDto.email}] was not registered.")
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
        val loginResponseDto: LoginResponseDto = userService.login(loginRequestDto).body!!

        // act
        val jwtToken: JWTToken = userService.refreshToken(loginResponseDto.refreshToken).body!!

        // assert
        jwtToken.apply {
            assertThat(jwtTokenProvider.verifyToken(token)).isEqualTo(true)
            assertThat(jwtTokenProvider.getUserPK(token)).isEqualTo(loginRequestDto.email)
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
        userService.login(loginRequestDto).body!!

        // act
        runCatching {
            userService.refreshToken("invalid-token").body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is UnauthorizedException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Failed when refresh token.")
        }
    }

    @Test
    fun is_checkValidUser_works_well() {
        // arrange
        userService.register(mockRegisterDto)
        val targetDid: String = userRepository.findByEmail(mockRegisterDto.email).did

        // act & assert
        runCatching {
            userService.checkValidUser(mockRegisterDto.email, targetDid)
        }.onFailure {
            fail("This should be failed.")
        }
    }

    @Test
    fun is_checkValidUser_works_when_incorrect_userEmail_and_did() {
        // arrange
        userService.register(mockRegisterDto)
        val targetDid: String = userRepository.findByEmail(mockRegisterDto.email).did

        val invalidEmail = "test@com"
        userService.register(mockRegisterDto.copy(email = invalidEmail, nickname = "new"))

        // act & assert
        runCatching {
            userService.checkValidUser(invalidEmail, targetDid)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ForbiddenException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [$invalidEmail] and DID were not matched.")
        }
    }

    @Test
    fun is_checkValidUser_works_on_unregistered_user() {
        // arrange
        userService.register(mockRegisterDto)
        val targetDid: String = userRepository.findByEmail(mockRegisterDto.email).did
        val invalidEmail = "test@com"

        // act & assert
        runCatching {
            userService.checkValidUser(invalidEmail, targetDid)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${invalidEmail}] was not registered.")
        }
    }

}
