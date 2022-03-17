package com.duckbox.domain.user

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserBoxRepository: MongoRepository<UserBox, Long> {
    fun findByEmail(email: String): UserBox
}
