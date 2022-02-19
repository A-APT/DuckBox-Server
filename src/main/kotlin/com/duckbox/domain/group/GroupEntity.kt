package com.duckbox.domain.group

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
    var menbers: Int,
    var profile: ObjectId? = null, // image
    var header: ObjectId? = null, // image
)

enum class GroupStatus {
    ALIVE, // [활성화]
    DELETED, // [삭제된]
    PENDING, // [인증전]
    REPORTED, // [신고된]
}
