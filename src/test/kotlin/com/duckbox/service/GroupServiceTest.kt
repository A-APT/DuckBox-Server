package com.duckbox.service

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.survey.SurveyRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.OverallDetailDto
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.group.GroupUpdateDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.service.ethereum.DIdService
import io.mockk.mockk
import io.mockk.mockkConstructor
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

@SpringBootTest
@ExtendWith(SpringExtension::class)
class GroupServiceTest {
    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userBoxRepository: UserBoxRepository

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var surveyRepository: SurveyRepository

    @Autowired
    private lateinit var surveyService: SurveyService

    private lateinit var mockDidService: DIdService

    private val mockGroupRegisterDto: GroupRegisterDto = MockDto.mockGroupRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"
    private val mockUserEmail2 = "email_2@konkuk.ac.kr"
    private val mockUserStudentId = 20191333

    @BeforeEach
    @AfterEach
    fun init() {
        groupRepository.deleteAll()
        photoRepository.deleteAll()
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        voteRepository.deleteAll()
        surveyRepository.deleteAll()
        mockkConstructor(DIdService::class)
        mockDidService = mockk(relaxed = true)
        setDidService(mockDidService)
    }

    // Set DidService
    private fun setDidService(didService: DIdService) {
        UserService::class.java.getDeclaredField("didService").apply {
            isAccessible = true
            set(userService, didService)
        }
    }

    fun registerMockUser() {
        userService.register(
            RegisterDto(
                studentId = mockUserStudentId,
                name = "je",
                password = "test",
                email = mockUserEmail,
                phoneNumber = "01012341234",
                nickname = "duck",
                college = "ku",
                department = listOf("computer", "software"),
                fcmToken = "temp",
                address = "0x11",
            )
        )
    }

    fun registerMockUser2() {
        userService.register(
            RegisterDto(
                studentId = mockUserStudentId,
                name = "je",
                password = "test",
                email = mockUserEmail2,
                phoneNumber = "01012341234",
                nickname = "duck!",
                college = "ku",
                department = listOf("computer", "software"),
                fcmToken = "temp",
                address = "0x11",
            )
        )
    }

    @Test
    fun is_getGroups_works_well() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()
        groupService.registerGroup(mockUserEmail, mockDto)

        // act
        val groupList: List<GroupDetailDto> = groupService.getGroups().body!!

        // assert
        assertThat(groupList.size).isEqualTo(1)
        assertThat(groupList[0].profile).isEqualTo(mockDto.profile)
        assertThat(groupList[0].header).isEqualTo(mockDto.header)
    }

    @Test
    fun is_registerGroup_works_well() {
        // act
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: ObjectId = ObjectId(groupService.registerGroup(mockUserEmail, mockDto).body!!)

        // assert
        groupRepository.findByName(mockGroupRegisterDto.name).apply {
            assertThat(id).isEqualTo(groupId)
            assertThat(name).isEqualTo(mockGroupRegisterDto.name)
            assertThat(description).isEqualTo(mockGroupRegisterDto.description)
            assertThat(profile).isEqualTo(mockGroupRegisterDto.profile)
            assertThat(header).isEqualTo(mockGroupRegisterDto.header)
            assertThat(status).isEqualTo(GroupStatus.PENDING)
        }
        userBoxRepository.findByEmail(mockUserEmail).apply {
            assertThat(groups[0]).isEqualTo(groupId)
        }
    }

    @Test
    fun is_registerGroup_works_multipartFile() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()

        // act
        groupService.registerGroup(mockUserEmail, mockDto)

        // assert
        groupRepository.findByName(mockDto.name).apply {
            assertThat(name).isEqualTo(mockDto.name)
            assertThat(leader).isEqualTo(mockDto.leader)
            assertThat(description).isEqualTo(mockDto.description)
            assertThat(status).isEqualTo(GroupStatus.PENDING)
            assertThat(photoRepository.findById(profile!!).isPresent).isEqualTo(true)
            assertThat(photoRepository.findById(header!!).isPresent).isEqualTo(true)
        }
    }

    @Test
    fun is_registerGroup_works_duplicate_group() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        groupService.registerGroup(mockUserEmail, mockDto)

        // act & assert
        runCatching {
            groupService.registerGroup(mockUserEmail, mockDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ConflictException)
            assertThat(it.message).isEqualTo("Group [${mockGroupRegisterDto.name}] is already registered.")
        }
    }

    @Test
    fun is_registerGroup_works_invalid_did() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = "invalid did")

        // act & assert
        runCatching {
            groupService.registerGroup(mockUserEmail, mockDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ForbiddenException)
            assertThat(it.message).isEqualTo("User [$mockUserEmail] and DID were not matched.")
        }
    }

    @Test
    fun is_updateGroup_works_well() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId,
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
            header = "changed header file!".toByteArray()
        )

        // act
        val updated: GroupEntity = groupService.updateGroup(mockUserEmail, mockGroupUpdateDto)

        // assert
        groupRepository.findById(ObjectId(groupId)).get().apply {
            assertThat(name).isEqualTo(mockGroupRegisterDto.name)
            assertThat(description).isEqualTo(mockGroupUpdateDto.description)
            assertThat(profile).isEqualTo(updated.profile)
            assertThat(header).isEqualTo(updated.header)
        }
        assertThat(photoRepository.findAll().size).isEqualTo(2)
    }

    @Test
    fun is_updateGroup_works_partial_update() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId,
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
        )

        // act
        val updated: GroupEntity = groupService.updateGroup(mockUserEmail, mockGroupUpdateDto)

        // assert
        groupRepository.findById(ObjectId(groupId)).get().apply {
            assertThat(name).isEqualTo(mockGroupRegisterDto.name)
            assertThat(description).isEqualTo(mockGroupUpdateDto.description)
            assertThat(profile).isEqualTo(updated.profile)
            assertThat(header).isEqualTo(updated.header)
        }
    }

    @Test
    fun is_updateGroup_works_with_photo() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        val mockGroupUpdateDto1 = GroupUpdateDto(
            id = groupId,
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
        )
        val mockGroupUpdateDto2 = GroupUpdateDto(
            id = groupId,
            description = null,
            profile = "double-changed profile file!".toByteArray(),
        )

        // act
        groupService.updateGroup(mockUserEmail, mockGroupUpdateDto1)
        val updated: GroupEntity = groupService.updateGroup(mockUserEmail, mockGroupUpdateDto2)

        // assert
        groupRepository.findById(ObjectId(groupId)).get().apply {
            assertThat(name).isEqualTo(mockGroupRegisterDto.name)
            assertThat(description).isEqualTo(mockGroupUpdateDto1.description)
            assertThat(profile).isEqualTo(updated.profile)
        }
        assertThat(photoRepository.findAll().size).isEqualTo(1)
    }

    @Test
    fun is_updateGroup_works_unregistered_group() {
        // arrange
        registerMockUser()
        val mockGroupUpdateDto = GroupUpdateDto(
            id = ObjectId().toString(),
            description = "changed description"
        )

        // act & assert
        runCatching {
            groupService.updateGroup(mockUserEmail, mockGroupUpdateDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${mockGroupUpdateDto.id}]")
        }
    }

    @Test
    fun is_updateGroup_works_invalid_did() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId,
            description = "changed description"
        )
        val invalidEmail = "not_a_leader@com"
        userService.register(
            RegisterDto(
                studentId = 2019333,
                name = "je",
                password = "test",
                email = invalidEmail,
                phoneNumber = "01012341234",
                nickname = "new",
                college = "ku",
                department = listOf("computer", "software"),
                fcmToken = "temp",
                address = "0x11",
            )
        )

        // act & assert
        runCatching {
            // the leader's did and invalidEmail's did is different
            groupService.updateGroup(invalidEmail, mockGroupUpdateDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ForbiddenException)
            assertThat(it.message).isEqualTo("User [$invalidEmail] and DID were not matched.")
        }
    }

    @Test
    fun is_joinGroup_works_well() {
        // arrange
        registerMockUser()
        registerMockUser2()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act
        groupService.joinGroup(mockUserEmail2, groupId)

        // assert
        userBoxRepository.findByEmail(mockUserEmail2).apply {
            assertThat(groups.size).isEqualTo(1)
            assertThat(groups[0]).isEqualTo(ObjectId(groupId))
        }
    }

    @Test
    fun is_joinGroup_works_on_invalid_user() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act & assert
        val invalidEmail = "test@com"
        runCatching {
            groupService.joinGroup(invalidEmail, groupId)
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
        registerMockUser()
        val invalidGroupId: String = ObjectId().toString()

        // act & assert
        runCatching {
            groupService.joinGroup(mockUserEmail, invalidGroupId)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${invalidGroupId}]")
        }
    }

    @Test
    fun is_leaveGroup_works_well() {
        // arrange
        registerMockUser()
        registerMockUser2()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        groupService.joinGroup(mockUserEmail2, groupId)

        // act
        groupService.leaveGroup(mockUserEmail2, groupId)

        // assert
        userBoxRepository.findByEmail(mockUserEmail2).apply {
            assertThat(groups.size).isEqualTo(0)
        }
    }

    @Test
    fun is_leaveGroup_throws_on_invalid_groupId() {
        // arrange
        registerMockUser()
        val invalidGroupId: String = ObjectId().toString()

        // act & assert
        runCatching {
            groupService.leaveGroup(mockUserEmail, invalidGroupId)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${invalidGroupId}]")
        }
    }

    @Test
    fun is_leaveGroup_throws_on_not_group_member() {
        // arrange
        registerMockUser()
        registerMockUser2()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act & assert
        runCatching {
            groupService.leaveGroup(mockUserEmail2, groupId)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [$mockUserEmail2] is not a group[$groupId] member.")
        }
    }

    @Test
    fun is_searchGroup_works_well() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val mockProfile: ByteArray = "profile file!".toByteArray()
        val mockHeader: ByteArray = "header file!".toByteArray()
        groupService.registerGroup(mockUserEmail, mockDto.copy(name = "hello duckbox", profile = mockProfile))
        groupService.registerGroup(mockUserEmail, mockDto.copy(name = "my first grouppp...", header = mockHeader))
        groupService.registerGroup(mockUserEmail, mockDto.copy(name = "this is my group!", header = mockHeader))

        // act
        val groupList1: List<GroupDetailDto> = groupService.searchGroup("woooo").body!!
        val groupList2: List<GroupDetailDto> = groupService.searchGroup("duckbox").body!!
        val groupList3: List<GroupDetailDto> = groupService.searchGroup("group").body!!

        // assert
        assertThat(groupList1.size).isEqualTo(0)
        assertThat(groupList2.size).isEqualTo(1)
        assertThat(groupList3.size).isEqualTo(2)

        assertThat(groupList2[0].profile).isEqualTo(mockProfile)
        assertThat(groupList3[0].header).isEqualTo(mockHeader)
    }


    @Test
    fun is_findGroupsByUser_works_well() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act
        val groupList: List<GroupDetailDto> = groupService.findGroupsOfUser(mockUserEmail).body!!

        // assert
        assertThat(groupList.size).isEqualTo(1)
        assertThat(groupList[0].profile).isEqualTo(mockDto.profile)
        assertThat(groupList[0].header).isEqualTo(mockDto.header)
    }

    @Test
    fun is_findGroupsByUser_works_well_on_not_group_owner() {
        // arrange
        registerMockUser()
        registerMockUser2()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        groupService.joinGroup(mockUserEmail2, groupId)

        // act
        val groupList: List<GroupDetailDto> = groupService.findGroupsOfUser(mockUserEmail2).body!!

        // assert
        assertThat(groupList.size).isEqualTo(1)
        assertThat(groupList[0].profile).isEqualTo(mockDto.profile)
        assertThat(groupList[0].header).isEqualTo(mockDto.header)
    }

    @Test
    fun is_findGroupsByUser_works_well_when_empty_joined_group() {
        // arrange
        registerMockUser()

        // act
        val groupList: List<GroupDetailDto> = groupService.findGroupsOfUser(mockUserEmail).body!!

        // assert
        assertThat(groupList.size).isEqualTo(0)
    }

    @Test
    fun is_findGroupById_works_well() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act
        val groupDetailDto: GroupDetailDto = groupService.findGroupById(groupId).body!!

        // assert
        assertThat(groupDetailDto.profile).isEqualTo(mockDto.profile)
        assertThat(groupDetailDto.header).isEqualTo(mockDto.header)
    }

    @Test
    fun is_findGroupById_throws_invalid_groupId() {
        // arrange
        registerMockUser()
        val invalidId: String = ObjectId().toString()

        // act & assert
        runCatching {
            groupService.findGroupById(invalidId)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${invalidId}]")
        }
    }

    @Test
    fun is_findGroupVoteAndSurveyOfUser_works_well_on_empty() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act
        val detailDtoList: List<OverallDetailDto> = groupService.findGroupVoteAndSurveyOfUser(mockUserEmail).body!!

        // assert
        assertThat(detailDtoList.size).isEqualTo(0)
    }

    @Test
    fun is_findGroupVoteAndSurveyOfUser_works_well_on_vote() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        val voteId1: String = voteService.registerVote(
            mockUserEmail,
            MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = null)).body!!
        val voteId2: String = voteService.registerVote(
            mockUserEmail,
            MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = null)).body!!
        val voteId3: String = voteService.registerVote(
            mockUserEmail,
            MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = listOf(mockUserStudentId))).body!!
        val voteId4: String = voteService.registerVote(
            mockUserEmail,
            MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = listOf(1, 2))).body!!
        userBoxRepository.findByEmail(mockUserEmail).apply {
            votes.add(ObjectId(voteId1))
            userBoxRepository.save(this)
        }

        // act
        val detailDtoList: List<OverallDetailDto> = groupService.findGroupVoteAndSurveyOfUser(mockUserEmail).body!!

        // assert
        assertThat(detailDtoList.size).isEqualTo(3)
        detailDtoList.find { it.id == voteId1 }!!.apply {
            assertThat(isVote).isEqualTo(true)
            assertThat(isAvailable).isEqualTo(false)
        }
        detailDtoList.find { it.id == voteId2 }!!.apply {
            assertThat(isVote).isEqualTo(true)
            assertThat(isAvailable).isEqualTo(true)
        }
        detailDtoList.find { it.id == voteId3 }!!.apply {
            assertThat(isVote).isEqualTo(true)
            assertThat(isAvailable).isEqualTo(true)
        }
        assertThat(detailDtoList.find { it.id == voteId4 }).isEqualTo(null)
    }

    @Test
    fun is_findGroupVoteAndSurveyOfUser_works_well_on_survey() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        val surveyId1: String = surveyService.registerSurvey(
            mockUserEmail,
            MockDto.mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = null)).body!!
        val surveyId2: String = surveyService.registerSurvey(
            mockUserEmail,
            MockDto.mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = null)).body!!
        val surveyId3: String = surveyService.registerSurvey(
            mockUserEmail,
            MockDto.mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = listOf(mockUserStudentId))).body!!
        val surveyId4: String = surveyService.registerSurvey(
            mockUserEmail,
            MockDto.mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = listOf(1, 2))).body!!
        userBoxRepository.findByEmail(mockUserEmail).apply {
            surveys.add(ObjectId(surveyId1))
            userBoxRepository.save(this)
        }

        // act
        val detailDtoList: List<OverallDetailDto> = groupService.findGroupVoteAndSurveyOfUser(mockUserEmail).body!!

        // assert
        assertThat(detailDtoList.size).isEqualTo(3)
        detailDtoList.find { it.id == surveyId1 }!!.apply {
            assertThat(isVote).isEqualTo(false)
            assertThat(isAvailable).isEqualTo(false)
        }
        detailDtoList.find { it.id == surveyId2 }!!.apply {
            assertThat(isVote).isEqualTo(false)
            assertThat(isAvailable).isEqualTo(true)
        }
        detailDtoList.find { it.id == surveyId3 }!!.apply {
            assertThat(isVote).isEqualTo(false)
            assertThat(isAvailable).isEqualTo(true)
        }
        assertThat(detailDtoList.find { it.id == surveyId4 }).isEqualTo(null)
    }

    @Test
    fun is_findGroupVoteAndSurveyOfUser_works_well_on_vote_and_survey() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        val voteId1: String = voteService.registerVote(
            mockUserEmail,
            MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = null)).body!!
        val voteId2: String = voteService.registerVote(
            mockUserEmail,
            MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = listOf(mockUserStudentId))).body!!
        val surveyId1: String = surveyService.registerSurvey(
            mockUserEmail,
            MockDto.mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = null)).body!!
        val surveyId2: String = surveyService.registerSurvey(
            mockUserEmail,
            MockDto.mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = listOf(mockUserStudentId))).body!!
        userBoxRepository.findByEmail(mockUserEmail).apply {
            votes.add(ObjectId(voteId1))
            userBoxRepository.save(this)
        }

        // act
        val detailDtoList: List<OverallDetailDto> = groupService.findGroupVoteAndSurveyOfUser(mockUserEmail).body!!

        // assert
        assertThat(detailDtoList.size).isEqualTo(4)
        detailDtoList.find { it.id == voteId1 }!!.apply {
            assertThat(isVote).isEqualTo(true)
            assertThat(isAvailable).isEqualTo(false)
        }
        detailDtoList.find { it.id == voteId2 }!!.apply {
            assertThat(isVote).isEqualTo(true)
            assertThat(isAvailable).isEqualTo(true)
        }
        detailDtoList.find { it.id == surveyId1 }!!.apply {
            assertThat(isVote).isEqualTo(false)
            assertThat(isAvailable).isEqualTo(true)
        }
        detailDtoList.find { it.id == surveyId2 }!!.apply {
            assertThat(isVote).isEqualTo(false)
            assertThat(isAvailable).isEqualTo(true)
        }
    }

}
