package com.duckbox.domain.vote

import com.duckbox.dto.vote.VoteDetailDto
import org.bson.types.Binary
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*
import javax.persistence.Id

@Document(collection="vote")
class VoteEntity (
    @Id
    var id: ObjectId = ObjectId(),

    var title: String,
    var content: String,
    var isGroup: Boolean,
    var owner: String, // groupId(ObjectId) if group vote else userId(Long)
    var startTime: Date,
    var finishTime: Date,
    var status: BallotStatus,
    var images: List<ObjectId>, // image list
    var candidates: List<String>,
    var voters: List<Int>, // student id
    var reward: Boolean
) {

    fun toVoteDetailDto(_images: List<ByteArray>): VoteDetailDto {
        return VoteDetailDto(
            id = id.toString(), // change ObjectId to String
            title, content, isGroup, owner, startTime, finishTime, status, _images, candidates, reward
        )
    }
}

enum class BallotStatus {
    OPEN,
    ONGOING,
    FINISHED,
}
