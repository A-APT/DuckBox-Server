package com.duckbox.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailAuthRepository: JpaRepository<EmailAuth, Long> {
    fun findByEmail(email: String): EmailAuth
}