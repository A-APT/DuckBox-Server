package com.duckbox.controller

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.service.UserService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.Test
import org.springframework.http.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
class GroupControllerTest {

    @LocalServerPort
    private var port: Int = -1

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private lateinit var baseAddress: String

    private val mockGroupRegisterDto: GroupRegisterDto = MockDto.mockGroupRegisterDto

    @BeforeEach
    @AfterEach
    fun initTest() {
        baseAddress = "http://localhost:${port}"
        groupRepository.deleteAll()
        photoRepository.deleteAll()
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
    fun is_registerGroup_works_no_headers_token() {
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/group/register", mockGroupRegisterDto, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_registerGroup_works_no_authToken() {
        // arrange
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer INVALID_TOKEN"
        }
        val httpEntity = HttpEntity<GroupRegisterDto>(mockGroupRegisterDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/register", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_registerGroup_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val httpEntity = HttpEntity<GroupRegisterDto>(mockGroupRegisterDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/register", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
    }

    @Test
    fun is_registerGroup_works_multipartFile() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy()
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()
        val httpEntity = HttpEntity<GroupRegisterDto>(mockDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/register", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
    }

}
