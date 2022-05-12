package com.duckbox.domain

import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
class UserRepositoryTest {
    @Autowired
    private lateinit var userRepository: UserRepository

    private val mockUser: User = User( // arrange
        did = "did.test",
        studentId = 2019333,
        name = "je",
        password = "test",
        email = "email@konkuk.ac.kr",
        phoneNumber = "01012341234",
        nickname = "duck",
        college = "ku",
        department = listOf("computer", "software"),
        roles = setOf("ROLE_USER"),
        fcmToken = "temp",
    )

    @BeforeEach
    @AfterEach
    fun init() {
        userRepository.deleteAll()
    }

    @Test
    fun is_save_and_singleFind_works_well() {
        // act
        userRepository.save(mockUser)

        // assert
        assertThat(userRepository.findByDid(mockUser.did).did).isEqualTo(mockUser.did)
        assertThat(userRepository.findByStudentId(mockUser.studentId).studentId).isEqualTo(mockUser.studentId)
        assertThat(userRepository.findByEmail(mockUser.email).email).isEqualTo(mockUser.email)
        assertThat(userRepository.findByPhoneNumber(mockUser.phoneNumber!!).phoneNumber).isEqualTo(mockUser.phoneNumber)
        assertThat(userRepository.findByNickname(mockUser.nickname).nickname).isEqualTo(mockUser.nickname)
    }

    @Test
    fun is_findUsers_works_well() {
        // act
        userRepository.save(mockUser)
        userRepository.save(mockUser) // for testing

        // assert
        assertThat(userRepository.findAllByCollege(mockUser.college).size).isEqualTo(2)
        assertThat(userRepository.findAllByDepartmentIn(listOf(mockUser.department[0])).size).isEqualTo(2)
    }

}
