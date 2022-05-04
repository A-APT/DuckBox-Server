package com.duckbox.domain.survey

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SurveyRepository: MongoRepository<SurveyEntity, ObjectId> {
    fun findByTitle(title: String): SurveyEntity
    fun findAllByGroupId(groupId: String): MutableList<SurveyEntity>
}
