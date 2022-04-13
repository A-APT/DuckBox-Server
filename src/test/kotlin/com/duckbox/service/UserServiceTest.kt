package com.duckbox.service

import BlindSecp256k1
import BlindedData
import Point
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
import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.LoginResponseDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.errors.exception.UnauthorizedException
import com.duckbox.security.JWTTokenProvider
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
import java.math.BigInteger

@SpringBootTest
@ExtendWith(SpringExtension::class)
class UserServiceTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var userBoxRepository: UserBoxRepository

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    @Autowired
    private lateinit var blindSecp256k1: BlindSecp256k1

    private val mockRegisterDto: RegisterDto = MockDto.mockRegisterDto

    @BeforeEach
    @AfterEach
    fun init() {
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        groupRepository.deleteAll()
        voteRepository.deleteAll()
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
    fun is_joinGroup_works_well() {
        // arrange
        userService.register(mockRegisterDto)
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockRegisterDto.email).did)
        val groupId: String = groupService.registerGroup(mockRegisterDto.email, mockDto).body!!

        // act
        userService.joinGroup(mockRegisterDto.email, groupId)

        // assert
        userBoxRepository.findByEmail(mockRegisterDto.email).apply {
            assertThat(groups.size).isEqualTo(1)
            assertThat(groups[0]).isEqualTo(ObjectId(groupId))
        }
    }

    @Test
    fun is_joinGroup_works_on_invalid_user() {
        // arrange
        userService.register(mockRegisterDto)
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockRegisterDto.email).did)
        val groupId: String = groupService.registerGroup(mockRegisterDto.email, mockDto).body!!

        // act & assert
        val invalidEmail = "test@com"
        runCatching {
            userService.joinGroup(invalidEmail, groupId)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${invalidEmail}] was not registered.")
        }
    }

    @Test
    fun is_joinGroup_works_on_invalid_groupId() {
        // arrange
        userService.register(mockRegisterDto)
        val invalidGroupId: String = ObjectId().toString()

        // act & assert
        runCatching {
            userService.joinGroup(mockRegisterDto.email, invalidGroupId)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${invalidGroupId}]")
        }
    }

    @Test
    fun is_joinVote_works_well() {
        // arrange
        userService.register(mockRegisterDto)
        val voteId: String = voteService.registerVote(mockRegisterDto.email, MockDto.mockVoteRegisterDto).body!!

        // act
        userService.joinVote(mockRegisterDto.email, voteId)

        // assert
        userBoxRepository.findByEmail(mockRegisterDto.email).apply {
            assertThat(votes.size).isEqualTo(1)
            assertThat(votes[0]).isEqualTo(ObjectId(voteId))
        }
    }

    @Test
    fun is_joinVote_works_on_invalid_user() {
        // arrange
        userService.register(mockRegisterDto)
        val voteId: String = voteService.registerVote(mockRegisterDto.email, MockDto.mockVoteRegisterDto).body!!
        val invalidEmail = "invalid email"

        // act & assert
        runCatching {
            userService.joinVote(invalidEmail, voteId)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            println(it)
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [$invalidEmail] was not registered.")
        }
    }

    @Test
    fun is_joinVote_works_on_invalid_groupId() {
        // arrange
        userService.register(mockRegisterDto)
        val invalidVoteId: String = ObjectId().toString()

        // act & assert
        runCatching {
            userService.joinVote(mockRegisterDto.email, invalidVoteId)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid VoteId: [${invalidVoteId}]")
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

    @Test
    fun is_findGroupsByUser_works_well() {
        // arrange
        userService.register(mockRegisterDto)
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockRegisterDto.email).did)
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()
        val groupId: String = groupService.registerGroup(mockRegisterDto.email, mockDto).body!!
        userService.joinGroup(mockRegisterDto.email, groupId)

        // act
        val groupList: List<GroupDetailDto> = userService.findGroupsByUser(mockRegisterDto.email).body!!

        // assert
        assertThat(groupList.size).isEqualTo(1)
        assertThat(groupList[0].profile).isEqualTo(mockDto.profile)
        assertThat(groupList[0].header).isEqualTo(mockDto.header)
    }

    @Test
    fun is_findGroupsByUser_works_well_when_empty_joined_group() {
        // arrange
        userService.register(mockRegisterDto)
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockRegisterDto.email).did)
        val groupId: String = groupService.registerGroup(mockRegisterDto.email, mockDto).body!!

        // act
        val groupList: List<GroupDetailDto> = userService.findGroupsByUser(mockRegisterDto.email).body!!

        // assert
        assertThat(groupList.size).isEqualTo(0)
    }

    private val R_: Point = Point(
        BigInteger("d80387d2861da050c1a8ae11c9a1ef5ed93572bd6537d50984c1dea2f2db912b", 16),
        BigInteger("edcef3840df9cd47256996c460f0ce045ccb4fac5e914f619c44ad642779011", 16)
    )
    private val pubkey: Point = Point(
        BigInteger("d7bf79fbdfa2c473d86d2f5fb325c05a3f9815c6b6e3bd7c1b61780651be8be7", 16),
        BigInteger("79a09b8427069518535389161410ae45643588fd945919b9f53f6e1a5b98554f", 16)
    )

    @Test
    fun is_generateBlindSigVoteToken_works_well() {
        // arrange
        userService.register(mockRegisterDto)
        val voteId: String = voteService.registerVote(mockRegisterDto.email, MockDto.mockVoteRegisterDto).body!!

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = blindedData.blindM.toString(16))

        // act
        val blindSigStr: String = userService.generateBlindSigVoteToken(mockRegisterDto.email, blindSigRequestDto).body!!
        val blindSig: BigInteger = BigInteger(blindSigStr, 16)
        val sig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, blindSig)

        // assert
        userBoxRepository.findByEmail(mockRegisterDto.email).apply {
            assertThat(votes.size).isEqualTo(1)
            assertThat(votes[0]).isEqualTo(ObjectId(voteId))
        }
        assertThat(blindSecp256k1.verify(sig, blindedData.R, message, pubkey)).isEqualTo(true)
    }
}
