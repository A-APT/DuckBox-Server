package com.duckbox.helper

import com.duckbox.MockDto
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.service.VoteService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*


@SpringBootTest
@ExtendWith(SpringExtension::class)
class VoteSchedulerTest {

    @Autowired
    private lateinit var voteScheduler: VoteScheduler

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var voteService: VoteService

    private val mockVoteRegisterDto: VoteRegisterDto = MockDto.mockVoteRegisterDto
    private val mockUserEmail = "email@konkuk.ac.kr"

    @BeforeEach
    @AfterEach
    fun init() {
        voteRepository.deleteAll()
        photoRepository.deleteAll()
    }

    fun registerMockUser() {
        userRepository.save(User(
            did = "", studentId = 1, name = "", password = "", email = mockUserEmail,
            phoneNumber = "", nickname = "", college = "", department = listOf(),
            roles = setOf("ROLE_USER")
        ))
    }

    @Test
    fun is_voteStatusTask_works_well() {
        // arrange
        registerMockUser()
        val now: Date = Date()
        val startTime = Date(now.time - 1000)
        val finishTime = now
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(startTime = startTime, finishTime = finishTime)
        val id = voteService.registerVote(mockUserEmail, mockDto)

        // act
        voteScheduler.voteStatusTask()
        voteRepository.findById(id).get().apply {
            Assertions.assertThat(status).isEqualTo(BallotStatus.ONGOING)
        }

        // assert
        voteScheduler.voteStatusTask()
        voteRepository.findById(id).get().apply {
            Assertions.assertThat(status).isEqualTo(BallotStatus.FINISHED)
        }
    }
}
