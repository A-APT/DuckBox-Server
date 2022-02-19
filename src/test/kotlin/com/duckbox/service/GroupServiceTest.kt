package com.duckbox.service

import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.dto.group.RegisterGroupDto
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
import org.springframework.mock.web.MockMultipartFile

@SpringBootTest
@ExtendWith(SpringExtension::class)
class GroupServiceTest {
    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupService: GroupService

    private val mockRegisterGroupDto = RegisterGroupDto(
        name = "testingGroup",
        leader = "did",
        description = "testing !",
        profile = null,
        header = null
    )

    @BeforeEach
    @AfterEach
    fun init() {
        groupRepository.deleteAll()
    }

    @Test
    fun is_registerGroup_works_well() {
        // act
        groupService.registerGroup(mockRegisterGroupDto)

        // assert
        groupRepository.findByName(mockRegisterGroupDto.name).apply {
            assertThat(name).isEqualTo(mockRegisterGroupDto.name)
            assertThat(leader).isEqualTo(mockRegisterGroupDto.leader)
            assertThat(description).isEqualTo(mockRegisterGroupDto.description)
            assertThat(profile).isEqualTo(mockRegisterGroupDto.profile)
            assertThat(header).isEqualTo(mockRegisterGroupDto.header)
            assertThat(status).isEqualTo(GroupStatus.PENDING)
            assertThat(menbers).isEqualTo(0)
        }
    }

    @Test
    fun is_registerGroup_works_multipartFile() {
        // arrange
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        mockRegisterGroupDto.profile = multipartFile
        mockRegisterGroupDto.header = multipartFile
        groupService.registerGroup(mockRegisterGroupDto)

        // assert
        groupRepository.findByName(mockRegisterGroupDto.name).apply {
            assertThat(name).isEqualTo(mockRegisterGroupDto.name)
            assertThat(leader).isEqualTo(mockRegisterGroupDto.leader)
            assertThat(description).isEqualTo(mockRegisterGroupDto.description)
            assertThat(status).isEqualTo(GroupStatus.PENDING)
            assertThat(menbers).isEqualTo(0)
        }
    }

    @Test
    fun is_registerGroup_works_duplicate_group() {
        // arrange
        groupService.registerGroup(mockRegisterGroupDto)

        // act & assert
        runCatching {
            groupService.registerGroup(mockRegisterGroupDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ConflictException)
            assertThat(it.message).isEqualTo("Group [${mockRegisterGroupDto.name}] is already registered.")
        }
    }

}
