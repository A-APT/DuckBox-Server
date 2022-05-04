package com.duckbox.service

import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.survey.SurveyEntity
import com.duckbox.domain.survey.SurveyRepository
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.dto.survey.SurveyRegisterDto
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
}
