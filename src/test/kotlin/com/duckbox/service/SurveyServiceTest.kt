package com.duckbox.service

import BlindSecp256k1
import BlindedData
import com.duckbox.DefinedValue
import com.duckbox.MockDto
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.survey.SurveyRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.dto.BlindSigToken
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.survey.SurveyDetailDto
import com.duckbox.dto.survey.SurveyRegisterDto
import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigInteger

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

    private var mockFcmService: FCMService = mockk(relaxed = true)

    private val mockSurveyRegisterDto: SurveyRegisterDto = MockDto.mockSurveyRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"
    private val mockUserEmail2 = "email_2@konkuk.ac.kr"
    private val mockStudentId = 2019333

    @BeforeEach
    @AfterEach
    fun init() {
        surveyRepository.deleteAll()
        photoRepository.deleteAll()
        userRepository.deleteAll()
        userBoxRepository.deleteAll()
        groupRepository.deleteAll()
        setFCMService() // set fcmService to mockFcmService
    }

    // Set private fcmService
    private fun setFCMService() {
        SurveyService::class.java.getDeclaredField("fcmService").apply {
            isAccessible = true
            set(surveyService, mockFcmService)
        }
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
                department = listOf("computer", "software"),
                fcmToken = "temp",
            )
        )
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
            )
        )
    }

    fun registerMockUserAndGroup(): String {
        registerMockUser()
        val mockDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        return groupService.registerGroup(mockUserEmail, mockDto).body!!
    }


    @Test
    fun is_registerSurvey_works_well_on_not_group_survey() {
        // act
        registerMockUser()
        val id: String = surveyService.registerSurvey(mockUserEmail, mockSurveyRegisterDto.copy(targets=null)).body!!

        // assert
        surveyRepository.findById(ObjectId(id)).get().apply {
            assertThat(title).isEqualTo(mockSurveyRegisterDto.title)
            assertThat(content).isEqualTo(mockSurveyRegisterDto.content)
            assertThat(isGroup).isEqualTo(mockSurveyRegisterDto.isGroup)
            assertThat(images.size).isEqualTo(0)
            assertThat(groupId).isEqualTo(null)
            assertThat(questions.size).isEqualTo(2)
            assertThat(targets).isEqualTo(null)
            assertThat(answerNum).isEqualTo(0)
            assertThat(owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
            assertThat(status).isEqualTo(BallotStatus.REGISTERED)
        }
    }

    @Test
    fun is_registerSurvey_works_well_on_group_survey() {
        // act
        val groupId: String = registerMockUserAndGroup()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(
            isGroup = true,
            groupId = groupId,
            images = listOf("test file!".toByteArray()))
        val id: String = surveyService.registerSurvey(mockUserEmail, mockDto).body!!

        // assert
        surveyRepository.findById(ObjectId(id)).get().apply {
            assertThat(title).isEqualTo(mockDto.title)
            assertThat(content).isEqualTo(mockDto.content)
            assertThat(isGroup).isEqualTo(mockDto.isGroup)
            assertThat(images.size).isEqualTo(1)
            assertThat(groupId).isEqualTo(groupId)
            assertThat(targets).isNotEqualTo(null)
            assertThat(answerNum).isEqualTo(0)
            assertThat(owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
        }
    }

    @Test
    fun is_registerSurvey_works_well_on_groupId_is_null_on_group_survey() {
        // arrange
        registerMockUser()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = null)

        // act & assert
        runCatching {
            surveyService.registerSurvey(mockUserEmail, mockDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${null}]")
        }
    }

    @Test
    fun is_registerSurvey_works_well_on_unregistered_group_survey() {
        // arrange
        registerMockUser()
        val invalidId = ObjectId().toString()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = invalidId)

        // act & assert
        runCatching {
            surveyService.registerSurvey(mockUserEmail, mockDto)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid GroupId: [${invalidId}]")
        }
    }

    @Test
    fun is_getAllSurveys_works_ok_when_empty() {
        // act
        val surveyList: List<SurveyDetailDto> = surveyService.getAllSurvey().body!!

        // assert
        assertThat(surveyList.size).isEqualTo(0)
    }

    @Test
    fun is_getAllSurvey_works_ok() {
        // arrange
        registerMockUser()
        val binaryFile: ByteArray = "test file!".toByteArray()
        val mockDto: SurveyRegisterDto = mockSurveyRegisterDto.copy(images = listOf(binaryFile))
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, mockDto).body!!

        // act
        val surveyList: List<SurveyDetailDto> = surveyService.getAllSurvey().body!!

        // assert
        assertThat(surveyList.size).isEqualTo(1)
        assertThat(surveyList[0].id).isEqualTo(surveyId)
        assertThat(surveyList[0].images[0]).isEqualTo(binaryFile)
        assertThat(surveyList[0].owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
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
        val surveyList: List<SurveyDetailDto> = surveyService.findSurveysOfGroup(groupId).body!!

        // assert
        assertThat(surveyList.size).isEqualTo(1)
        assertThat(surveyList[0].id).isEqualTo(surveyId)
        assertThat(surveyList[0].images[0]).isEqualTo(binaryFile)
        assertThat(surveyList[0].owner).isEqualTo(userRepository.findByEmail(mockUserEmail).nickname)
        assertThat(surveyList[0].targets!!.size).isEqualTo(mockSurveyRegisterDto.targets!!.size)
    }

    @Test
    fun is_findSurveysOfGroup_works_well_when_unregistered_group() {
        // act
        val surveyList: List<SurveyDetailDto> = surveyService.findSurveysOfGroup(ObjectId().toString()).body!!

        // assert
        assertThat(surveyList.size).isEqualTo(0)
    }
    
    @Test
    fun is_generateBlindSigSurveyToken_works_well() {
        // arrange
        registerMockUser()
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, MockDto.mockSurveyRegisterDto).body!!

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(DefinedValue.R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = surveyId, blindMessage = blindedData.blindM.toString(16))

        // act
        val surveyToken: BlindSigToken = surveyService.generateBlindSigSurveyToken(mockUserEmail, blindSigRequestDto).body!!
        val serverBSig: BigInteger = BigInteger(surveyToken.serverBSig, 16)
        val ownerBSig: BigInteger = BigInteger(surveyToken.ownerBSig, 16)
        val serverSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, serverBSig)
        val surveyOwnerSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, ownerBSig)

        // assert
        userBoxRepository.findByEmail(mockUserEmail).apply {
            assertThat(surveys.size).isEqualTo(1)
            assertThat(surveys[0]).isEqualTo(ObjectId(surveyId))
        }
        assertThat(blindSecp256k1.verify(serverSig, blindedData.R, message, DefinedValue.pubkey)).isEqualTo(true)
        assertThat(blindSecp256k1.verify(surveyOwnerSig, blindedData.R, message, DefinedValue.ownerPublic)).isEqualTo(true)
        assertThat(surveyRepository.findById(ObjectId(surveyId)).get().answerNum).isEqualTo(1)
    }

    @Test
    fun is_generateBlindSigSurveyToken_works_well_when_group_survey_not_specified_targets() {
        // arrange
        registerMockUser() // create user
        val mockGroupDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockGroupDto).body!! // create group
        val mockSurveyDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = null)
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, mockSurveyDto).body!! // create survey
        groupService.joinGroup(mockUserEmail, groupId) // join group

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(DefinedValue.R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = surveyId, blindMessage = blindedData.blindM.toString(16))

        // act
        val surveyToken: BlindSigToken = surveyService.generateBlindSigSurveyToken(mockUserEmail, blindSigRequestDto).body!!
        val serverBSig: BigInteger = BigInteger(surveyToken.serverBSig, 16)
        val ownerBSig: BigInteger = BigInteger(surveyToken.ownerBSig, 16)
        val serverSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, serverBSig)
        val surveyOwnerSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, ownerBSig)

        // assert
        userBoxRepository.findByEmail(mockUserEmail).apply {
            assertThat(surveys.size).isEqualTo(1)
            assertThat(surveys[0]).isEqualTo(ObjectId(surveyId))
        }
        assertThat(blindSecp256k1.verify(serverSig, blindedData.R, message, DefinedValue.pubkey)).isEqualTo(true)
        assertThat(blindSecp256k1.verify(surveyOwnerSig, blindedData.R, message, DefinedValue.ownerPublic)).isEqualTo(true)
    }

    @Test
    fun is_generateBlindSigSurveyToken_works_well_when_group_survey_specified_targets() {
        // arrange
        registerMockUser() // create user
        val mockGroupDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockGroupDto).body!! // create group
        val mockSurveyDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = listOf(mockStudentId))
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, mockSurveyDto).body!! // create survey
        // userService.joinGroup(mockRegisterDto.email, groupId) // join group // not required

        val message: ByteArray = "test".encodeToByteArray()
        val blindedData: BlindedData = blindSecp256k1.blind(DefinedValue.R_, message)
        val blindSigRequestDto = BlingSigRequestDto(targetId = surveyId, blindMessage = blindedData.blindM.toString(16))

        // act
        val surveyToken: BlindSigToken = surveyService.generateBlindSigSurveyToken(mockUserEmail, blindSigRequestDto).body!!
        val serverBSig: BigInteger = BigInteger(surveyToken.serverBSig, 16)
        val ownerBSig: BigInteger = BigInteger(surveyToken.ownerBSig, 16)
        val serverSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, serverBSig)
        val surveyOwnerSig: BigInteger = blindSecp256k1.unblind(blindedData.a, blindedData.b, ownerBSig)

        // assert
        userBoxRepository.findByEmail(mockUserEmail).apply {
            assertThat(surveys.size).isEqualTo(1)
            assertThat(surveys[0]).isEqualTo(ObjectId(surveyId))
        }
        assertThat(blindSecp256k1.verify(serverSig, blindedData.R, message, DefinedValue.pubkey)).isEqualTo(true)
        assertThat(blindSecp256k1.verify(surveyOwnerSig, blindedData.R, message, DefinedValue.ownerPublic)).isEqualTo(true)
    }

    @Test
    fun is_generateBlindSigSurveyToken_works_invalid_user() {
        // arrange
        registerMockUser() // create user
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, mockSurveyRegisterDto).body!!
        val blindSigRequestDto = BlingSigRequestDto(targetId = surveyId, blindMessage = "")

        // act & assert
        val invalidEmail = "test@com"
        runCatching {
            surveyService.generateBlindSigSurveyToken(invalidEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${invalidEmail}] was not registered.")
        }
    }

    @Test
    fun is_generateBlindSigSurveyToken_works_invalid_survey() {
        // arrange
        registerMockUser() // create user
        val blindSigRequestDto = BlingSigRequestDto(targetId = ObjectId().toString(), blindMessage = "")

        // act & assert
        runCatching {
            surveyService.generateBlindSigSurveyToken(mockUserEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Invalid SurveyId: [${blindSigRequestDto.targetId}]")
        }
    }

    @Test
    fun is_generateBlindSigSurveyToken_works_user_already_get_token() {
        // arrange
        registerMockUser() // create user
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, MockDto.mockSurveyRegisterDto).body!!

        val blindSigRequestDto = BlingSigRequestDto(targetId = surveyId, blindMessage = "12345")
        surveyService.generateBlindSigSurveyToken(mockUserEmail, blindSigRequestDto) // get token

        // act & assert
        runCatching {
            surveyService.generateBlindSigSurveyToken(mockUserEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ConflictException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${mockUserEmail}] has already participated in the survey [${blindSigRequestDto.targetId}].")
        }
    }

    @Test
    fun is_generateBlindSigSurveyToken_works_user_is_not_a_group_member() {
        // arrange
        registerMockUser() // create user
        registerMockUser2() // create another user: not a group menber
        val mockGroupDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockGroupDto).body!! // create group
        val mockSurveyDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = null)
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, mockSurveyDto).body!! // create survey

        val blindSigRequestDto = BlingSigRequestDto(targetId = surveyId, blindMessage = "")

        // act & assert
        runCatching {
            surveyService.generateBlindSigSurveyToken(mockUserEmail2, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ForbiddenException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${mockUserEmail2}] is ineligible for survey [${blindSigRequestDto.targetId}].")
        }
    }

    @Test
    fun is_generateBlindSigSurveyToken_works_user_is_not_in_targets() {
        // arrange
        registerMockUser() // create user
        val mockGroupDto: GroupRegisterDto = MockDto.mockGroupRegisterDto.copy(leader = userRepository.findByEmail(mockUserEmail).did)
        val groupId: String = groupService.registerGroup(mockUserEmail, mockGroupDto).body!! // create group
        val mockSurveyDto = mockSurveyRegisterDto.copy(isGroup = true, groupId = groupId, targets = listOf(1, 2))
        val surveyId: String = surveyService.registerSurvey(mockUserEmail, mockSurveyDto).body!! // create survey

        val blindSigRequestDto = BlingSigRequestDto(targetId = surveyId, blindMessage = "")

        // act & assert
        runCatching {
            surveyService.generateBlindSigSurveyToken(mockUserEmail, blindSigRequestDto).body!!
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is ForbiddenException).isEqualTo(true)
            assertThat(it.message).isEqualTo("User [${mockUserEmail}] is ineligible for survey [${blindSigRequestDto.targetId}].")
        }
    }
}