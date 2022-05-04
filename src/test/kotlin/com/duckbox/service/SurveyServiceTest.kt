package com.duckbox.service

import BlindSecp256k1
import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.survey.SurveyRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.survey.SurveyDetailDto
import com.duckbox.dto.survey.SurveyRegisterDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.errors.exception.NotFoundException
import org.assertj.core.api.Assertions
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
class SurveyServiceTest {
    @Autowired
    private lateinit var surveyRepository: SurveyRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var surveyService: SurveyService

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userBoxRepository: UserBoxRepository

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var blindSecp256k1: BlindSecp256k1

    private val mockSurveyRegisterDto: SurveyRegisterDto = MockDto.mockSurveyRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"
    private val mockStudentId = 2019333

    @BeforeEach
    @AfterEach
    fun init() {
        surveyRepository.deleteAll()
        photoRepository.deleteAll()
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        groupRepository.deleteAll()
    }

    fun registerMockUser() {
        userService.register(
            RegisterDto(
                studentId = mockStudentId,
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
        return groupService.registerGroup(mockUserEmail, mockDto).body!!
    }


    @Test
    fun is_registerSurvey_works_well_on_not_group_vote() {
        // act
        registerMockUser()
        val id: String = surveyService.registerSurvey(mockUserEmail, mockSurveyRegisterDto.copy(targets=null)).body!!

        // assert
        surveyRepository.findById(ObjectId(id)).get().apply {
            Assertions.assertThat(title).isEqualTo(mockSurveyRegisterDto.title)
            Assertions.assertThat(content).isEqualTo(mockSurveyRegisterDto.content)
            Assertions.assertThat(isGroup).isEqualTo(mockSurveyRegisterDto.isGroup)
            Assertions.assertThat(images.size).isEqualTo(0)
            Assertions.assertThat(groupId).isEqualTo(null)
            Assertions.assertThat(questions.size).isEqualTo(2)
            Assertions.assertThat(targets).isEqualTo(null)
            Assertions.assertThat(answerNum).isEqualTo(0)
            Assertions.assertThat(owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
            Assertions.assertThat(status).isEqualTo(BallotStatus.REGISTERED)
        }
    }

    @Test
    fun is_registerSurvey_works_well_on_group_vote() {
        // act
        val groupId: String = registerMockUserAndGroup()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(
            isGroup = true,
            groupId = groupId,
            images = listOf("test file!".toByteArray()))
        val id: String = surveyService.registerSurvey(mockUserEmail, mockDto).body!!

        // assert
        surveyRepository.findById(ObjectId(id)).get().apply {
            Assertions.assertThat(title).isEqualTo(mockDto.title)
            Assertions.assertThat(content).isEqualTo(mockDto.content)
            Assertions.assertThat(isGroup).isEqualTo(mockDto.isGroup)
            Assertions.assertThat(images.size).isEqualTo(1)
            Assertions.assertThat(groupId).isEqualTo(groupId)
            Assertions.assertThat(targets).isNotEqualTo(null)
            Assertions.assertThat(answerNum).isEqualTo(0)
            Assertions.assertThat(owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
        }
    }

    @Test
    fun is_registerSurvey_works_well_on_groupId_is_null_on_group_vote() {
        // arrange
        registerMockUser()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = null)

        // act & assert
        runCatching {
            surveyService.registerSurvey(mockUserEmail, mockDto)
        }.onSuccess {
            Assertions.fail("This should be failed.")
        }.onFailure {
            Assertions.assertThat(it is NotFoundException).isEqualTo(true)
            Assertions.assertThat(it.message).isEqualTo("Invalid GroupId: [${null}]")
        }
    }

    @Test
    fun is_registerSurvey_works_well_on_unregistered_group_vote() {
        // arrange
        registerMockUser()
        val invalidId = ObjectId().toString()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = invalidId)

        // act & assert
        runCatching {
            surveyService.registerSurvey(mockUserEmail, mockDto)
        }.onSuccess {
            Assertions.fail("This should be failed.")
        }.onFailure {
            Assertions.assertThat(it is NotFoundException).isEqualTo(true)
            Assertions.assertThat(it.message).isEqualTo("Invalid GroupId: [${invalidId}]")
        }
    }

    @Test
    fun is_getAllSurveys_works_ok_when_empty() {
        // act
        val surveyList: List<SurveyDetailDto> = surveyService.getAllSurvey().body!!

        // assert
        Assertions.assertThat(surveyList.size).isEqualTo(0)
    }

    @Test
    fun is_getAllSurvey_works_ok() {
        // arrange
        registerMockUser()
        val binaryFile: ByteArray = "test file!".toByteArray()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(images = listOf(binaryFile))
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, mockDto).body!!

        // act
        val voteList: List<SurveyDetailDto> = surveyService.getAllSurvey().body!!

        // assert
        Assertions.assertThat(voteList.size).isEqualTo(1)
        Assertions.assertThat(voteList[0].id).isEqualTo(surveyId)
        Assertions.assertThat(voteList[0].images[0]).isEqualTo(binaryFile)
        Assertions.assertThat(voteList[0].owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
    }

    @Test
    fun is_findSurveysOfGroup_works_ok() {
        // arrange
        val groupId: String = registerMockUserAndGroup()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId)
        val binaryFile: ByteArray = "test file!".toByteArray()
        mockDto.images = listOf(binaryFile)
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, mockDto).body!!

        // act
        val voteList: List<SurveyDetailDto> = surveyService.findSurveysOfGroup(groupId).body!!

        // assert
        Assertions.assertThat(voteList.size).isEqualTo(1)
        Assertions.assertThat(voteList[0].id).isEqualTo(surveyId)
        Assertions.assertThat(voteList[0].images[0]).isEqualTo(binaryFile)
        Assertions.assertThat(voteList[0].owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
        Assertions.assertThat(voteList[0].targets!!.size).isEqualTo(mockSurveyRegisterDto.targets!!.size)
    }

    @Test
    fun is_findSurveysOfGroup_works_well_when_unregistered_group() {
        // act
        val surveyList: List<SurveyDetailDto> = surveyService.findSurveysOfGroup(ObjectId().toString()).body!!

        // assert
        Assertions.assertThat(surveyList.size).isEqualTo(0)
    }

}