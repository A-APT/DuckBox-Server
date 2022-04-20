package com.duckbox.domain.vote

import com.duckbox.dto.vote.VoteDetailDto
import org.bson.types.Binary
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.util.*
import javax.persistence.Id

@Document(collection="vote")
class VoteEntity (
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
    var candidates: List<String>,
    var voters: List<Int>?, // student id. null if isGroup is false or all group member have right to vote
    var voteNum: Int, // count the number of people who voted
    var reward: Boolean
) {

    fun toVoteDetailDto(_images: List<ByteArray>): VoteDetailDto {
        return VoteDetailDto(
            id = id.toString(), // change ObjectId to String
            title, content, isGroup, groupId, owner, startTime, finishTime, status, _images, candidates, voters, reward
        )
    }
}

enum class BallotStatus {
    REGISTERED,
    OPEN,
    FINISHED,
}
