package com.duckbox.service

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.errors.exception.ConflictException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
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
            assertThat(menbers).isEqualTo(0)
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
            assertThat(menbers).isEqualTo(0)
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

}
