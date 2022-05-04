package com.duckbox.service

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.group.GroupUpdateDto
import com.duckbox.dto.user.RegisterDto
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

    private val mockGroupRegisterDto: GroupRegisterDto = MockDto.mockGroupRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun init() {
        groupRepository.deleteAll()
        photoRepository.deleteAll()
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
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
        groupService.registerGroup(mockUserEmail, mockDto)

        // assert
        groupRepository.findByName(mockGroupRegisterDto.name).apply {
            assertThat(name).isEqualTo(mockGroupRegisterDto.name)
            assertThat(description).isEqualTo(mockGroupRegisterDto.description)
            assertThat(profile).isEqualTo(mockGroupRegisterDto.profile)
            assertThat(header).isEqualTo(mockGroupRegisterDto.header)
            assertThat(status).isEqualTo(GroupStatus.PENDING)
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
                department = listOf("computer", "software")
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
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act
        groupService.joinGroup(mockUserEmail, groupId)

        // assert
        userBoxRepository.findByEmail(mockUserEmail).apply {
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
        groupService.joinGroup(mockUserEmail, groupId)

        // act
        val groupList: List<GroupDetailDto> = groupService.findGroupsOfUser(mockUserEmail).body!!

        // assert
        assertThat(groupList.size).isEqualTo(1)
        assertThat(groupList[0].profile).isEqualTo(mockDto.profile)
        assertThat(groupList[0].header).isEqualTo(mockDto.header)
    }

    @Test
    fun is_findGroupsByUser_works_well_when_empty_joined_group() {
        // arrange
        registerMockUser()
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act
        val groupList: List<GroupDetailDto> = groupService.findGroupsOfUser(mockUserEmail).body!!

        // assert
        assertThat(groupList.size).isEqualTo(0)
    }

}
