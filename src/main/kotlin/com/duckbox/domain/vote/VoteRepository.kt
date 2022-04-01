package com.duckbox.domain.vote

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository: MongoRepository<VoteEntity, ObjectId> {
    fun findByTitle(title: String): VoteEntity
    fun findAllByStatus(status: BallotStatus): MutableList<VoteEntity>
    fun findAllByOwner(ownerId: String): MutableList<VoteEntity>
}
