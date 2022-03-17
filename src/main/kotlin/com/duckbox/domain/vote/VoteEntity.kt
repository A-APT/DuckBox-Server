package com.duckbox.domain.vote

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
    var groupId: ObjectId?, // required if isGroup is true
    var startTime: Date,
    var finishTime: Date,
    var images: List<ObjectId>, // image list
    var candidates: List<String>,
    var voters: List<Int>, // student id
    var reward: Boolean
)
