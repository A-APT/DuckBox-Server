package com.duckbox.domain.survey

import com.duckbox.domain.vote.BallotStatus
import com.duckbox.dto.survey.SurveyDetailDto
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.util.*
import javax.persistence.Id

@Document(collection="survey")
class SurveyEntity (
    @Id
    var id: ObjectId = ObjectId(),

    var title: String,
    var content: String,
    var isGroup: Boolean,
    var groupId: String?, // groupId(ObjectId) if isGroup is true
    var owner: String, // owner's nickname
    var ownerPrivate: BigInteger, // private key
    var startTime: Date,
    var finishTime: Date,
    var status: BallotStatus,
    var images: List<ObjectId>, // image list
    var questions: List<Question>,
    var targets: List<Int>?, // student id. null if isGroup is false or all group member have right to survey
    var answerNum: Int, // count the number of people who answered
    var reward: Boolean
) {

    fun toSurveyDetailDto(_images: List<ByteArray>): SurveyDetailDto {
        return SurveyDetailDto(
            id = id.toString(), // change ObjectId to String
            title, content, isGroup, groupId, owner, startTime, finishTime, status, _images, questions, targets, reward
        )
    }
}
