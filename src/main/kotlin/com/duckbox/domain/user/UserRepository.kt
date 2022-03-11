package com.duckbox.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: JpaRepository<User, Long> {
    fun findByDid(did: String): User
    fun findByStudentId(studentId: Int): User
    fun findByEmail(email: String): User
    fun findByPhoneNumber(phoneNumber: String): User
    fun findByNickname(nickname: String): User
    fun findAllByCollege(college: String): MutableList<User>
    fun findAllByDepartmentIn(department: List<String>) : MutableList<User>
}
