package com.duckbox.domain.photo

import org.bson.types.Binary
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import javax.persistence.Id

@Document(collection="photo")
class Photo (
    @Id
    var id: ObjectId = ObjectId(),
    var image: Binary
)