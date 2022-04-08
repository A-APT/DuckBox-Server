package com.duckbox.service

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.bson.types.Binary
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

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
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    private val mockVoteRegisterDto: VoteRegisterDto = MockDto.mockVoteRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun init() {
        voteRepository.deleteAll()
        photoRepository.deleteAll()
        userRepository.deleteAll()
        groupRepository.deleteAll()
    }

    fun registerMockUser() {
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
    }

    fun registerMockUserAndGroup(): String {
        registerMockUser()
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        return groupService.registerGroup(mockUserEmail, mockDto).toString()
    }

    @Test
    fun is_registerVote_works_well_on_not_group_vote() {
        // act
        registerMockUser()
        val id = voteService.registerVote(mockUserEmail, mockVoteRegisterDto.copy(voters=null))

        // assert
        voteRepository.findById(id).get().apply {
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
        val id = voteService.registerVote(mockUserEmail, mockDto)

        // assert
        voteRepository.findById(id).get().apply {
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
        val id = voteService.registerVote(mockUserEmail, mockDto)

        // assert
        voteRepository.findById(id).get().apply {
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
        val voteId: ObjectId = voteService.registerVote(mockUserEmail, mockDto)

        // act
        val voteList: List<VoteDetailDto> = voteService.getAllVote().body!!

        // assert
        assertThat(voteList.size).isEqualTo(1)
        assertThat(voteList[0].id).isEqualTo(voteId.toString())
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
        val voteId: ObjectId = voteService.registerVote(mockUserEmail, mockDto)

        // act
        val voteList: List<VoteDetailDto> = voteService.findVotesOfGroup(groupId).body!!

        // assert
        assertThat(voteList.size).isEqualTo(1)
        assertThat(voteList[0].id).isEqualTo(voteId.toString())
        assertThat(voteList[0].images[0]).isEqualTo(binaryFile)
        assertThat(voteList[0].owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
    }

    @Test
    fun is_findVotesOfGroup_works_well_when_unregistered_group() {
        // act
        val voteList: List<VoteDetailDto> = voteService.findVotesOfGroup(ObjectId().toString()).body!!

        // assert
        assertThat(voteList.size).isEqualTo(0)
    }
}
