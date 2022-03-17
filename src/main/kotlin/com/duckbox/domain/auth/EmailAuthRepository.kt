package com.duckbox.domain.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailAuthRepository: JpaRepository<EmailAuth, Long> {
    fun findByEmail(email: String): EmailAuth
}