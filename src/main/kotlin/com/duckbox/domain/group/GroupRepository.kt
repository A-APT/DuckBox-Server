package com.duckbox.domain.group

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository: MongoRepository<GroupEntity, Long> {
    fun findByName(name: String): GroupEntity
    fun findByStatus(status: GroupStatus): GroupEntity
}
