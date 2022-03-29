package com.duckbox.domain.group

import com.duckbox.dto.group.GroupDetailDto
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import javax.persistence.*

@Document(collection="group")
class GroupEntity (
    @Id
    var id: ObjectId = ObjectId(),
    var name: String,
    var leader: String, // did
    var status: GroupStatus,
    var description: String,
    var profile: ObjectId? = null, // image
    var header: ObjectId? = null, // image
) {
    fun toGroupDetailDto(): GroupDetailDto {
        return GroupDetailDto(
            id = id.toString(), // change ObjectId to String
            name, leader, status, description
        )
    }
}

enum class GroupStatus {
    PENDING, // [인증전]
    VALID, // [활성화]
    DELETED, // [삭제된]
    REPORTED, // [신고된]
}
