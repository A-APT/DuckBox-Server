package com.duckbox.service

import BlindSecp256k1
import BlindedData
import com.duckbox.DefinedValue
import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.dto.vote.VoteToken
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.math.BigInteger

@SpringBootTest
@ExtendWith(SpringExtension::class)
class VoteServiceTest {
    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userBoxRepository: UserBoxRepository

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var blindSecp256k1: BlindSecp256k1

    private val mockVoteRegisterDto: VoteRegisterDto = MockDto.mockVoteRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"
    private val mockStudentId = 2019333

    @BeforeEach
    @AfterEach
    fun init() {
        voteRepository.deleteAll()
        photoRepository.deleteAll()
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        groupRepository.deleteAll()
    }

    fun registerMockUser() {
        userService.register(
            RegisterDto(
                studentId = mockStudentId,
                name = "je",
                password = "test",
                email = mockUserEmail,
                phoneNumber = "01012341234",
                nickname = "duck",
                college = "ku",
                department = listOf("computer", "software")
            )
        )
    }

    fun registerMockUserAndGroup(): String {
        registerMockUser()
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        return groupService.registerGroup(mockUserEmail, mockDto).body!!
    }

    @Test
    fun is_registerVote_works_well_on_not_group_vote() {
        // act
        registerMockUser()
        val id: String = voteService.registerVote(mockUserEmail, mockVoteRegisterDto.copy(voters=null)).body!!

        // assert
        voteRepository.findById(ObjectId(id)).get().apply {
            assertThat(title).isEqualTo(mockVoteRegisterDto.title)
            assertThat(content).isEqualTo(mockVoteRegisterDto.content)
            assertThat(isGroup).isEqualTo(mockVoteRegisterDto.isGroup)
            assertThat(images.size).isEqualTo(0)
            assertThat(groupId).isEqualTo(null)
            assertThat(voters).isEqualTo(null)
            assertThat(owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
            assertThat(status).isEqualTo(BallotStatus.REGISTERED)
        }
    }

    @Test
    fun is_registerVote_works_well_on_group_vote() {
        // act
        val groupId: String = registerMockUserAndGroup()
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(isGroup = true, groupId = groupId)
        val id: String = voteService.registerVote(mockUserEmail, mockDto).body!!

        // assert
        voteRepository.findById(ObjectId(id)).get().apply {
            assertThat(title).isEqualTo(mockDto.title)
            assertThat(content).isEqualTo(mockDto.content)
            assertThat(isGroup).isEqualTo(mockDto.isGroup)
            assertThat(images.size).isEqualTo(0)
            assertThat(groupId).isEqualTo(groupId)
            assertThat(voters).isNotEqualTo(null)
            assertThat(owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
        }
    }

    @Test
    fun is_registerVote_works_well_on_groupId_is_null_on_group_vote() {
        // arrange
        registerMockUser()
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(isGroup = true, groupId = null)

        // act & assert
        runCatching {
            voteService.registerVote(mockUserEmail, mockDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${null}]")
        }
    }

    @Test
    fun is_registerVote_works_well_on_unregistered_group_vote() {
        // arrange
        registerMockUser()
        val invalidId = ObjectId().toString()
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(isGroup = true, groupId = invalidId)

        // act & assert
        runCatching {
            voteService.registerVote(mockUserEmail, mockDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${invalidId}]")
        }
    }

    @Test
    fun is_registerVote_works_multipartFile() {
        // arrange
        registerMockUser()
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy()
        mockDto.images = listOf("test file!".toByteArray())

        // act
        val id: String = voteService.registerVote(mockUserEmail, mockDto).body!!

        // assert
        voteRepository.findById(ObjectId(id)).get().apply {
            assertThat(title).isEqualTo(mockDto.title)
            assertThat(content).isEqualTo(mockDto.content)
            assertThat(isGroup).isEqualTo(mockDto.isGroup)
            assertThat(images.size).isEqualTo(1)
            assertThat(photoRepository.findById(images[0]).isPresent).isEqualTo(true)
        }
    }

    @Test
    fun is_getAllVote_works_ok_when_empty() {
        // act
        val voteList: List<VoteDetailDto> = voteService.getAllVote().body!!

        // assert
        assertThat(voteList.size).isEqualTo(0)
    }

    @Test
    fun is_getAllVote_works_ok() {
        // arrange
        registerMockUser()
        val binaryFile: ByteArray = "test file!".toByteArray()
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(images = listOf(binaryFile))
        val voteId: String = voteService.registerVote(mockUserEmail, mockDto).body!!

        // act
        val voteList: List<VoteDetailDto> = voteService.getAllVote().body!!

        // assert
        assertThat(voteList.size).isEqualTo(1)
        assertThat(voteList[0].id).isEqualTo(voteId)
        assertThat(voteList[0].images[0]).isEqualTo(binaryFile)
        assertThat(voteList[0].owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
    }

    @Test
    fun is_findVotesOfGroup_works_ok() {
        // arrange
        val groupId: String = registerMockUserAndGroup()
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(isGroup = true, groupId = groupId)
        val binaryFile: ByteArray = "test file!".toByteArray()
        mockDto.images = listOf(binaryFile)
        val voteId: String = voteService.registerVote(mockUserEmail, mockDto).body!!

        // act
        val voteList: List<VoteDetailDto> = voteService.findVotesOfGroup(groupId).body!!

        // assert
        assertThat(voteList.size).isEqualTo(1)
        assertThat(voteList[0].id).isEqualTo(voteId)
        assertThat(voteList[0].images[0]).isEqualTo(binaryFile)
        assertThat(voteList[0].owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
        assertThat(voteList[0].voters!!.size).isEqualTo(mockVoteRegisterDto.voters!!.size)
    }

    @Test
    fun is_findVotesOfGroup_works_well_when_unregistered_group() {
        // act
        val voteList: List<VoteDetailDto> = voteService.findVotesOfGroup(ObjectId().toString()).body!!

        // assert
        assertThat(voteList.size).isEqualTo(0)
    }

    @Test
    fun is_generateBlindSigVoteToken_works_well() {
        // arrange
        registerMockUser()
        val voteId: String = voteService.registerVote(mockUserEmail, MockDto.mockVoteRegisterDto).body!!

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(DefinedValue.R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = blindedData.blindM.toString(16))

        // act
        val voteToken: VoteToken = voteService.generateBlindSigVoteToken(mockUserEmail, blindSigRequestDto).body!!
        val serverToken: BigInteger = BigInteger(voteToken.serverToken, 16)
        val voteOwnerToken: BigInteger = BigInteger(voteToken.voteOwnerToken, 16)
        val serverSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, serverToken)
        val voteOwnerSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, voteOwnerToken)

        // assert
        userBoxRepository.findByEmail(mockUserEmail).apply {
            assertThat(votes.size).isEqualTo(1)
            assertThat(votes[0]).isEqualTo(ObjectId(voteId))
        }
        assertThat(blindSecp256k1.verify(serverSig, blindedData.R, message, DefinedValue.pubkey)).isEqualTo(true)
        assertThat(blindSecp256k1.verify(voteOwnerSig, blindedData.R, message, DefinedValue.voteOwnerPublic)).isEqualTo(true)
    }

    @Test
    fun is_generateBlindSigVoteToken_works_well_when_group_vote_not_specified_voters() {
        // arrange
        registerMockUser() // create user
        val mockGroupDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockGroupDto).body!! // create group
        val mockVoteDto = MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = null)
        val voteId: String = voteService.registerVote(mockUserEmail, mockVoteDto).body!! // create vote
        userService.joinGroup(mockUserEmail, groupId) // join group

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(DefinedValue.R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = blindedData.blindM.toString(16))

        // act
        val voteToken: VoteToken = voteService.generateBlindSigVoteToken(mockUserEmail, blindSigRequestDto).body!!
        val serverToken: BigInteger = BigInteger(voteToken.serverToken, 16)
        val voteOwnerToken: BigInteger = BigInteger(voteToken.voteOwnerToken, 16)
        val serverSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, serverToken)
        val voteOwnerSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, voteOwnerToken)

        // assert
        userBoxRepository.findByEmail(mockUserEmail).apply {
            assertThat(votes.size).isEqualTo(1)
            assertThat(votes[0]).isEqualTo(ObjectId(voteId))
        }
        assertThat(blindSecp256k1.verify(serverSig, blindedData.R, message, DefinedValue.pubkey)).isEqualTo(true)
        assertThat(blindSecp256k1.verify(voteOwnerSig, blindedData.R, message, DefinedValue.voteOwnerPublic)).isEqualTo(true)
    }

    @Test
    fun is_generateBlindSigVoteToken_works_well_when_group_vote_specified_voters() {
        // arrange
        registerMockUser() // create user
        val mockGroupDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockGroupDto).body!! // create group
        val mockVoteDto = MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = listOf(mockStudentId))
        val voteId: String = voteService.registerVote(mockUserEmail, mockVoteDto).body!! // create vote
        // userService.joinGroup(mockRegisterDto.email, groupId) // join group // not required

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(DefinedValue.R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = blindedData.blindM.toString(16))

        // act
        val voteToken: VoteToken = voteService.generateBlindSigVoteToken(mockUserEmail, blindSigRequestDto).body!!
        val serverToken: BigInteger = BigInteger(voteToken.serverToken, 16)
        val voteOwnerToken: BigInteger = BigInteger(voteToken.voteOwnerToken, 16)
        val serverSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, serverToken)
        val voteOwnerSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, voteOwnerToken)

        // assert
        userBoxRepository.findByEmail(mockUserEmail).apply {
            assertThat(votes.size).isEqualTo(1)
            assertThat(votes[0]).isEqualTo(ObjectId(voteId))
        }
        assertThat(blindSecp256k1.verify(serverSig, blindedData.R, message, DefinedValue.pubkey)).isEqualTo(true)
        assertThat(blindSecp256k1.verify(voteOwnerSig, blindedData.R, message, DefinedValue.voteOwnerPublic)).isEqualTo(true)
    }

    @Test
    fun is_generateBlindSigVoteToken_works_invalid_user() {
        // arrange
        registerMockUser() // create user
        val voteId: String = voteService.registerVote(mockUserEmail, MockDto.mockVoteRegisterDto).body!!
        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = "")

        // act & assert
        val invalidEmail = "test@com"
        runCatching {
            voteService.generateBlindSigVoteToken(invalidEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${invalidEmail}] was not registered.")
        }
    }

    @Test
    fun is_generateBlindSigVoteToken_works_invalid_vote() {
        // arrange
        registerMockUser() // create user
        val blindSigRequestDto = BlingSigRequestDto(targetId = ObjectId().toString(), blindMessage = "")

        // act & assert
        runCatching {
            voteService.generateBlindSigVoteToken(mockUserEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid VoteId: [${blindSigRequestDto.targetId}]")
        }
    }

    @Test
    fun is_generateBlindSigVoteToken_works_user_already_get_token() {
        // arrange
        registerMockUser() // create user
        val voteId: String = voteService.registerVote(mockUserEmail, MockDto.mockVoteRegisterDto).body!!

        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = "12345")
        voteService.generateBlindSigVoteToken(mockUserEmail, blindSigRequestDto) // get token

        // act & assert
        runCatching {
            voteService.generateBlindSigVoteToken(mockUserEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ConflictException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${mockUserEmail}] has already participated in the vote [${blindSigRequestDto.targetId}].")
        }
    }

    @Test
    fun is_generateBlindSigVoteToken_works_user_is_not_a_group_member() {
        // arrange
        registerMockUser() // create user
        val mockGroupDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockGroupDto).body!! // create group
        val mockVoteDto = MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = null)
        val voteId: String = voteService.registerVote(mockUserEmail, mockVoteDto).body!! // create vote

        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = "")

        // act & assert
        runCatching {
            voteService.generateBlindSigVoteToken(mockUserEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ForbiddenException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${mockUserEmail}] is ineligible for vote [${blindSigRequestDto.targetId}].")
        }
    }

    @Test
    fun is_generateBlindSigVoteToken_works_user_is_not_in_voters() {
        // arrange
        registerMockUser() // create user
        val mockGroupDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockGroupDto).body!! // create group
        val mockVoteDto = MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = listOf(1, 2))
        val voteId: String = voteService.registerVote(mockUserEmail, mockVoteDto).body!! // create vote

        val blindSigRequestDto = BlingSigRequestDto(targetId = voteId, blindMessage = "")

        // act & assert
        runCatching {
            voteService.generateBlindSigVoteToken(mockUserEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ForbiddenException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${mockUserEmail}] is ineligible for vote [${blindSigRequestDto.targetId}].")
        }
    }
}
