package com.duckbox.controller

import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.survey.SurveyRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.OverallDetailDto
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.group.GroupUpdateDto
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.service.GroupService
import com.duckbox.service.SurveyService
import com.duckbox.service.UserService
import com.duckbox.service.VoteService
import org.assertj.core.api.Assertions
import org.bson.types.ObjectId
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
    private lateinit var userBoxRepository: UserBoxRepository

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var surveyRepository: SurveyRepository

    @Autowired
    private lateinit var surveyService: SurveyService

    private lateinit var baseAddress: String

    private val mockGroupRegisterDto: GroupRegisterDto = MockDto.mockGroupRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"
    private val mockUserEmail2 = "email_2@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun initTest() {
        baseAddress = "http://localhost:${port}"
        groupRepository.deleteAll()
        photoRepository.deleteAll()
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        voteRepository.deleteAll()
        surveyRepository.deleteAll()
    }

    fun registerAndLogin(): String {
        userService.register(
            RegisterDto(
            studentId = 2019333,
            name = "je",
            password = "test",
            email = mockUserEmail,
            phoneNumber = "01012341234",
            nickname = "duck",
            college = "ku",
            department = listOf("computer", "software"),
            fcmToken = "temp",
            address = "0x11",
        ))
        return userService.login(
            LoginRequestDto(email = mockUserEmail, password = "test")
        ).body!!.token
    }

    fun registerMockUser2() {
        userService.register(
            RegisterDto(
                studentId = 2019333,
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
    fun is_getGroups_works_no_headers_token() {
        // act, assert
        restTemplate
            .getForEntity("${baseAddress}/api/v1/groups/all", Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_getGroups_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        groupService.registerGroup(mockUserEmail, mockDto)
        val httpEntity = HttpEntity(null, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/groups/all", HttpMethod.GET, httpEntity, Array<GroupDetailDto>::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
                Assertions.assertThat(body!!.size).isEqualTo(1)
            }
    }

    @Test
    fun is_registerGroup_works_no_headers_token() {
        // act, assert
        restTemplate
            .postForEntity("${baseAddress}/api/v1/group", mockGroupRegisterDto, Unit::class.java)
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
            .exchange("${baseAddress}/api/v1/group", HttpMethod.POST, httpEntity, Unit::class.java)
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
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)

        val httpEntity = HttpEntity<GroupRegisterDto>(mockDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group", HttpMethod.POST, httpEntity, String::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_registerGroup_works_multipartFile() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()
        val httpEntity = HttpEntity<GroupRegisterDto>(mockDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group", HttpMethod.POST, httpEntity, String::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_updateGroup_works_no_authToken() {
        // arrange
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer INVALID_TOKEN"
        }

        val mockGroupUpdateDto = GroupUpdateDto(
            id = ObjectId().toString(),
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
        )
        val httpEntity = HttpEntity<GroupUpdateDto>(mockGroupUpdateDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group", HttpMethod.PUT, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_registerGroup_works_invalid_did() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = "invalid did")

        val httpEntity = HttpEntity<GroupRegisterDto>(mockDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_updateGroup_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        val mockGroupUpdateDto = GroupUpdateDto(
            id = groupId,
            description = "changed description",
            profile = "changed profile file!".toByteArray(),
        )
        val httpEntity = HttpEntity<GroupUpdateDto>(mockGroupUpdateDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group", HttpMethod.PUT, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
    }

    @Test
    fun is_updateGroup_works_when_unregistered_group() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }

        val mockGroupUpdateDto = GroupUpdateDto(
            id = ObjectId().toString(),
            description = "changed description",
        )
        val httpEntity = HttpEntity<GroupUpdateDto>(mockGroupUpdateDto, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group", HttpMethod.PUT, httpEntity, NotFoundException::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }


    @Test
    fun is_joinGroup_works_no_authToken_header() {
        // arrange
        val httpEntity = HttpEntity(ObjectId().toString(), HttpHeaders())

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/member", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_joinGroup_works_well() {
        // arrange
        val token: String = registerAndLogin()
        registerMockUser2()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail2).did)
        val groupId: String = groupService.registerGroup(mockUserEmail2, mockDto).body!! // mockUser 2
        val httpEntity = HttpEntity(groupId, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/member", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NO_CONTENT)
                println(this.body)
            }
    }

    @Test
    fun is_joinGroup_works_on_invalid_user() {
        // arrange
        val token: String = registerAndLogin()
        registerMockUser2()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer invalid_token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail2).did)
        val groupId = groupService.registerGroup(mockUserEmail2, mockDto)
        val httpEntity = HttpEntity(groupId.toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/member", HttpMethod.POST, httpEntity, Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_joinGroup_works_on_invalid_groupId() {
        // arrange
        val token: String = registerAndLogin()
        registerMockUser2()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val httpEntity = HttpEntity(ObjectId().toString(), httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/member", HttpMethod.POST, httpEntity, NotFoundException::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    fun is_searchGroup_works_no_headers_token() {
        // act, assert
        val query = "test"
        restTemplate
            .getForEntity("${baseAddress}/api/v1/groups/$query", Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_searchGroup_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        groupService.registerGroup(mockUserEmail, mockDto.copy(name = "hello duckbox"))
        groupService.registerGroup(mockUserEmail, mockDto.copy(name = "my first grouppp..."))
        groupService.registerGroup(mockUserEmail, mockDto.copy(name = "this is my group!"))
        val httpEntity = HttpEntity(null, httpHeaders)
        val query: String = "group"

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/groups/$query", HttpMethod.GET, httpEntity, Array<GroupDetailDto>::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
                Assertions.assertThat(body!!.size).isEqualTo(2)
            }
    }

    @Test
    fun is_findGroupsByUser_throws_when_no_headers_token() {
        // act, assert
        restTemplate
            .getForEntity("${baseAddress}/api/v1/groups", Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_findGroupsByUser_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        val httpEntity = HttpEntity(null, httpHeaders)

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/groups", HttpMethod.GET, httpEntity, Array<GroupDetailDto>::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
                Assertions.assertThat(body!!.size).isEqualTo(1)
            }
    }

    @Test
    fun is_findGroupById_works_no_headers_token() {
        // act, assert
        val invalidId: String = ObjectId().toString()
        restTemplate
            .getForEntity("${baseAddress}/api/v1/group/$invalidId", Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_findGroupById_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val httpEntity = HttpEntity(null, httpHeaders)

        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        mockDto.profile = "profile file!".toByteArray()
        mockDto.header = "header file!".toByteArray()
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/$groupId", HttpMethod.GET, httpEntity, GroupDetailDto::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
            }
    }

    @Test
    fun is_findGroupById_throws_invalid_groupId() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val httpEntity = HttpEntity(null, httpHeaders)
        val invalidId: String = ObjectId().toString()

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/group/$invalidId", HttpMethod.GET, httpEntity, NotFoundException::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    fun is_findGroupVoteAndSurveyOfUser_throws_when_no_headers_token() {
        // act, assert
        restTemplate
            .getForEntity("${baseAddress}/api/v1/groups/items", Unit::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            }
    }

    @Test
    fun is_findGroupVoteAndSurveyOfUser_works_well() {
        // arrange
        val token: String = registerAndLogin()
        val httpHeaders = HttpHeaders().apply {
            this["Authorization"] = "Bearer $token"
        }
        val httpEntity = HttpEntity(null, httpHeaders)
        val mockDto: GroupRegisterDto = mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockDto).body!!
        val voteId1: String = voteService.registerVote(
            mockUserEmail,
            MockDto.mockVoteRegisterDto.copy(isGroup = true, groupId = groupId, voters = null)).body!!
        val surveyId1: String = surveyService.registerSurvey(
            mockUserEmail,
            MockDto.mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = null)).body!!

        // act, assert
        restTemplate
            .exchange("${baseAddress}/api/v1/groups/items", HttpMethod.GET, httpEntity, Array<OverallDetailDto>::class.java)
            .apply {
                Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
                Assertions.assertThat(body!!.size).isEqualTo(2)
            }
    }
}
