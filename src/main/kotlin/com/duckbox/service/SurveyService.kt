package com.duckbox.service

import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.survey.SurveyEntity
import com.duckbox.domain.survey.SurveyRepository
import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserBox
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteEntity
import com.duckbox.dto.BlindSigToken
import com.duckbox.dto.survey.SurveyDetailDto
import com.duckbox.dto.survey.SurveyRegisterDto
import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigInteger

@Service
class SurveyService (
    private val surveyRepository: SurveyRepository,
    private val photoService: PhotoService,
    private val userRepository: UserRepository,
    private val userBoxRepository: UserBoxRepository,
    private val groupRepository: GroupRepository,
    private val blindSignatureService: BlindSignatureService,
){
    fun registerSurvey(userEmail: String, surveyRegisterDto: SurveyRegisterDto): ResponseEntity<String> {
        val owner: String = userRepository.findByEmail(userEmail).nickname
        if (surveyRegisterDto.isGroup) { // check groupId is valid
            runCatching {
                groupRepository.findById(ObjectId(surveyRegisterDto.groupId)).get()
            }.onFailure {
                throw NotFoundException("Invalid GroupId: [${surveyRegisterDto.groupId}]")
            }
        }

        // upload images if exists
        val idOfImages: MutableList<ObjectId> = mutableListOf()
        surveyRegisterDto.images.forEach {
            idOfImages.add(photoService.savePhoto(it))
        }

        // save to server
        val id = surveyRepository.save(
            SurveyEntity(
                title = surveyRegisterDto.title,
                content = surveyRegisterDto.content,
                isGroup = surveyRegisterDto.isGroup,
                groupId = surveyRegisterDto.groupId,
                owner = owner,
                ownerPrivate = BigInteger(surveyRegisterDto.ownerPrivate, 16),
                startTime = surveyRegisterDto.startTime,
                finishTime = surveyRegisterDto.finishTime,
                status = BallotStatus.REGISTERED,
                images = idOfImages.toList(),
                questions = surveyRegisterDto.questions,
                targets = surveyRegisterDto.targets,
                answerNum = 0,
                reward = surveyRegisterDto.reward
            )
        ).id

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                id.toString()
            )
    }

    fun getAllSurvey(): ResponseEntity<List<SurveyDetailDto>> {
        val surveyList: MutableList<SurveyDetailDto> = mutableListOf()
        surveyRepository.findAll().forEach {
            // get images
            val images: MutableList<ByteArray> = mutableListOf()
            it.images.forEach { photoId ->
                images.add(photoService.getPhoto(photoId).data)
            }
            surveyList.add(it.toSurveyDetailDto(images))
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                surveyList
            )
    }

    fun findSurveysOfGroup(_groupId: String): ResponseEntity<List<SurveyDetailDto>> {
        val groupId: ObjectId = ObjectId(_groupId)
        val surveyList: MutableList<SurveyDetailDto> = mutableListOf()
        surveyRepository.findAllByGroupId(groupId.toString()).forEach {
            // get images
            val images: MutableList<ByteArray> = mutableListOf()
            it.images.forEach { photoId ->
                images.add(photoService.getPhoto(photoId).data)
            }
            surveyList.add(it.toSurveyDetailDto(images))
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                surveyList
            )
    }

    fun generateBlindSigSurveyToken(userEmail: String, blindSigRequestDto: BlingSigRequestDto): ResponseEntity<BlindSigToken> {
        val surveyObjectId = ObjectId(blindSigRequestDto.targetId)

        // Find user
        lateinit var user: User
        runCatching {
            userRepository.findByEmail(userEmail)
        }.onSuccess {
            user = it
        }.onFailure {
            throw NotFoundException("User [${userEmail}] was not registered.")
        }
        val userBox: UserBox = userBoxRepository.findByEmail(userEmail)

        // Find survey entity
        lateinit var survey: SurveyEntity
        runCatching {
            surveyRepository.findById(surveyObjectId).get()
        }.onSuccess {
            survey = it
        }.onFailure {
            throw NotFoundException("Invalid SurveyId: [${blindSigRequestDto.targetId}]")
        }

        // check whether user already received survey token (signature)
        if (userBox.surveys.find { it == surveyObjectId } != null) {
            throw ConflictException("User [$userEmail] has already participated in the survey [${blindSigRequestDto.targetId}].")
        }

        // check user is eligible
        if (survey.isGroup) {
            if (survey.targets == null) {
                // all group member have right to survey
                if (userBox.groups.find { it == ObjectId(survey.groupId) } == null) {
                    // but ineligible when user is not a group member
                    throw ForbiddenException("User [$userEmail] is ineligible for survey [${blindSigRequestDto.targetId}].")
                }
            } else if (survey.targets?.find { it == user.studentId } == null) {
                // only survey.targets have right to survey
                throw ForbiddenException("User [$userEmail] is ineligible for survey [${blindSigRequestDto.targetId}].")
            }
        } // else, all user have right to survey (== community)

        // generate 2 blind signatures: sign with Server's key and SurveyOwner's key
        val blindMessage: BigInteger = BigInteger(blindSigRequestDto.blindMessage, 16)
        val blindSigOfServer: BigInteger = blindSignatureService.blindSig(blindMessage)
        val blindSigOfSurveyOwner: BigInteger = blindSignatureService.blindSig(survey.ownerPrivate, blindMessage)

        // update user's survey record
        userBox.surveys.add(surveyObjectId)
        userBoxRepository.save(userBox)

        // update survey's answerNum record
        survey.answerNum++
        surveyRepository.save(survey)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                BlindSigToken( // in radix 16
                    serverBSig = blindSigOfServer.toString(16),
                    ownerBSig = blindSigOfSurveyOwner.toString(16)
                )
            )
    }
}
