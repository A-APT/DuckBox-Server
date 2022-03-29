package com.duckbox.service

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.group.GroupUpdateDto
import com.duckbox.errors.exception.ConflictException
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
    private lateinit var groupService: GroupService

    private val mockGroupRegisterDto: GroupRegisterDto = MockDto.mockGroupRegisterDto

    @BeforeEach
    @AfterEach
    fun init() {
        groupRepository.deleteAll()
        photoRepository.deleteAll()
    }

    @Test
    fun is_getGroups_works_well() {
        // arrange
        groupService.registerGroup(mockGroupRegisterDto)

        // act
        val groupList: List<GroupDetailDto> = groupService.getGroups().body!!

        // assert
        assertThat(groupList.size).isEqualTo(1)
    }

    @Test
    fun is_registerGroup_works_well() {
        // act
        groupService.registerGroup(mockGroupRegisterDto)

        // assert
        groupRepository.findByName(mockGroupRegisterDto.name).apply {
            assertThat(name).isEqualTo(mockGroupRegisterDto.name)
            assertThat(leader).isEqualTo(mockGroupRegisterDto.leader)
            assertThat(description).isEqualTo(mockGroupRegisterDto.description)
            assertThat(profile).isEqualTo(mockGroupRegisterDto.profile)
            assertThat(header).isEqualTo(mockGroupRegisterDto.header)
            assertThat(status).isEqualTo(GroupStatus.PENDING)
        }
    }

    @Test
    fun is_registerGroup_works_multipartFile() {
        // arrange
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy()
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()

        // act
        groupService.registerGroup(mockDto)

        // assert
        groupRepository.findByName(mockDto.name).apply {
            assertThat(name).isEqualTo(mockDto.name)
            assertThat(leader).isEqualTo(mockDto.leader)
            assertThat(description).isEqualTo(mockDto.description)
            assertThat(status).isEqualTo(GroupStatus.PENDING)
            assertThat(photoRepository.findById(profile).isPresent).isEqualTo(true)
            assertThat(photoRepository.findById(header).isPresent).isEqualTo(true)
        }
    }

    @Test
    fun is_registerGroup_works_duplicate_group() {
        // arrange
        groupService.registerGroup(mockGroupRegisterDto)

        // act & assert
        runCatching {
            groupService.registerGroup(mockGroupRegisterDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ConflictException)
            assertThat(it.message).isEqualTo("Group [${mockGroupRegisterDto.name}] is already registered.")
        }
    }

    @Test
    fun is_updateGroup_works_well() {
        // arrange
        val groupId: ObjectId = groupService.registerGroup(mockGroupRegisterDto)
        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId.toString(),
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
            header = "changed header file!".toByteArray()
        )

        // act
        val updated: GroupEntity = groupService.updateGroup(mockGroupUpdateDto)

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
        val groupId: ObjectId = groupService.registerGroup(mockGroupRegisterDto)
        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId.toString(),
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
        )

        // act
        val updated: GroupEntity = groupService.updateGroup(mockGroupUpdateDto)

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
        val groupId: ObjectId = groupService.registerGroup(mockGroupRegisterDto)
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
        groupService.updateGroup(mockGroupUpdateDto1)
        val updated: GroupEntity = groupService.updateGroup(mockGroupUpdateDto2)

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
        val mockGroupUpdateDto = GroupUpdateDto(
            id = ObjectId().toString(),
            description = "changed description"
        )

        // act & assert
        runCatching {
            groupService.updateGroup(mockGroupUpdateDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException)
            assertThat(it.message).isEqualTo("Group [${mockGroupUpdateDto.id}] was not registered.")
        }
    }

}
