package com.duckbox.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SMSAuthRepository: JpaRepository<SMSAuth, Long> {
    fun findByPhoneNumber(phoneNumber: String): SMSAuth
}
