package com.duckbox.domain.user

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import javax.persistence.Id

@Document(collection="userbox")
class UserBox (
    @Id
    var id: Long, // == User.id
    var email: String, // == User.email
    var groups: MutableList<ObjectId>,
    var votes: MutableList<ObjectId>,
    // var surveys: MutableList<ObjectId>
)
