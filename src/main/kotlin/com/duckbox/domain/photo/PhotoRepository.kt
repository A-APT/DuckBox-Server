package com.duckbox.domain.photo

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PhotoRepository: MongoRepository<Photo, ObjectId> {
}
