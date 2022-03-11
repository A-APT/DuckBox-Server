package com.duckbox.controller

import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.group.RegisterGroupDto
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.service.GroupService
import com.duckbox.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity.status
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
class GroupControllerTest {

    @LocalServerPort
    private var port: Int = -1

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private lateinit var baseAddress: String

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc : MockMvc

    private val mockRegisterGroupDto = RegisterGroupDto(
        name = "testingGroup",
        leader = "did",
        description = "testing !",
        profile = null,
        header = null
    )

    @BeforeEach
    @AfterEach
    fun initTest() {
        baseAddress = "http://localhost:${port}"
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    fun registerAndLogin(): String {
        userService.register(
            RegisterDto(
            studentId = 2019333,
            name = "je",
            password = "test",
            email = "email@konkuk.ac.kr",
            phoneNumber = "01012341234",
            nickname = "duck",
            college = "ku",
            department = listOf("computer", "software")
        ))
        return userService.login(
            LoginRequestDto(email = "email@konkuk.ac.kr", password = "test")
        ).body!!.token
    }

    @Test
    fun is_registerGroup_works_no_authToken() {
        // act, assert
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/group/register")
                .param("name", mockRegisterGroupDto.name)
                .param("leader", mockRegisterGroupDto.leader)
                .param("description", mockRegisterGroupDto.description)
        ).andExpect { status(HttpStatus.NO_CONTENT) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
            }
    }

    @Test
    fun is_registerGroup_works_well() {
        // arrange
        val token: String = registerAndLogin()

        // act, assert
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/group/register")
                .header("Authorization", "Bearer $token")
                .param("name", mockRegisterGroupDto.name)
                .param("leader", mockRegisterGroupDto.leader)
                .param("description", mockRegisterGroupDto.description)
        ).andExpect { status(HttpStatus.NO_CONTENT) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.NO_CONTENT.value())
            }
    }

    @Test
    fun is_registerGroup_works_multipartFile() {
        // arrange
        val token: String = registerAndLogin()

        // act, assert
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/group/register")
                .file("profile", "profile: test file!".toByteArray())
                .file("header", "header: test file!".toByteArray())
                .param("name", mockRegisterGroupDto.name)
                .param("leader", mockRegisterGroupDto.leader)
                .param("description", mockRegisterGroupDto.description)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect { status(HttpStatus.NO_CONTENT) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.NO_CONTENT.value())
            }
    }

}
