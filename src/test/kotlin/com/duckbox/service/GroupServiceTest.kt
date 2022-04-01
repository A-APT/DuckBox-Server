package com.duckbox.service

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.domain.photo.PhotoRepository
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
        groupService.registerGroup(mockUserEmail, mockDto)

        // act
        val groupList: List<GroupDetailDto> = groupService.getGroups().body!!

        // assert
        assertThat(groupList.size).isEqualTo(1)
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
        val groupId: ObjectId = groupService.registerGroup(mockUserEmail, mockDto)
        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId.toString(),
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
            header = "changed header file!".toByteArray()
        )

        // act
        val updated: GroupEntity = groupService.updateGroup(mockUserEmail, mockGroupUpdateDto)

        // assert
        groupRepository.findById(groupId).get().apply {
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
        val groupId: ObjectId = groupService.registerGroup(mockUserEmail, mockDto)
        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId.toString(),
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
        )

        // act
        val updated: GroupEntity = groupService.updateGroup(mockUserEmail, mockGroupUpdateDto)

        // assert
        groupRepository.findById(groupId).get().apply {
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
        val groupId: ObjectId = groupService.registerGroup(mockUserEmail, mockDto)
        val mockGroupUpdateDto1 = GroupUpdateDto(
            id = groupId.toString(),
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
        )
        val mockGroupUpdateDto2 = GroupUpdateDto(
            id = groupId.toString(),
            description = null,
            profile = "double-changed profile file!".toByteArray(),
        )

        // act
        groupService.updateGroup(mockUserEmail, mockGroupUpdateDto1)
        val updated: GroupEntity = groupService.updateGroup(mockUserEmail, mockGroupUpdateDto2)

        // assert
        groupRepository.findById(groupId).get().apply {
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
        val groupId: ObjectId = groupService.registerGroup(mockUserEmail, mockDto)
        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId.toString(),
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
                nickname = "duck",
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
}
