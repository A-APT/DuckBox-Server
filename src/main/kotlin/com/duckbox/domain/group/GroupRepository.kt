package com.duckbox.domain.group

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository: JpaRepository<GroupEntity, Long> {
    fun findByName(name: String): GroupEntity
    fun findByStatus(status: GroupStatus): GroupEntity
}
