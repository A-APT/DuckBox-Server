package com.duckbox.helper

import com.duckbox.MockDto
import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.service.VoteService
import com.duckbox.service.ethereum.BallotService
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.bson.types.ObjectId
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

    private lateinit var mockBallotService: BallotService

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
        userRepository.deleteAll()
        photoRepository.deleteAll()
        mockkConstructor(BallotService::class)
        mockBallotService = mockk(relaxed = true)
        setBallotService(mockBallotService)
    }

    fun registerMockUser() {
        userRepository.save(User(
            did = "", studentId = 1, name = "", password = "", email = mockUserEmail,
            phoneNumber = "", nickname = "", college = "", department = listOf(),
            roles = setOf("ROLE_USER"),
            fcmToken = "temp",
        ))
    }

    // Set ballotService
    private fun setBallotService(ballotService: BallotService) {
        VoteScheduler::class.java.getDeclaredField("ballotService").apply {
            isAccessible = true
            set(voteScheduler, ballotService)
        }
    }

    @Test
    fun is_voteStatusTask_works_well() {
        // arrange
        registerMockUser()
        val now: Date = Date()
        val startTime = Date(now.time - 1000)
        val finishTime = now
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(startTime = startTime, finishTime = finishTime)
        val id: String = voteService.registerVote(mockUserEmail, mockDto).body!!

        // act & assert: start Ballot
        voteScheduler.voteStatusTask()
        voteRepository.findById(ObjectId(id)).get().apply {
            Assertions.assertThat(status).isEqualTo(BallotStatus.OPEN)
        }

        // act & assert: close Ballot
        voteScheduler.voteStatusTask()
        voteRepository.findById(ObjectId(id)).get().apply {
            Assertions.assertThat(status).isEqualTo(BallotStatus.FINISHED)
        }
    }

    @Test
    fun is_voteStatusTask_works_well_when_before_the_startTime() {
        // arrange
        registerMockUser()
        val now: Date = Date()
        val startTime = Date(now.time + 1000000)
        val finishTime = Date(now.time + 100000000)
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(startTime = startTime, finishTime = finishTime)
        val id: String = voteService.registerVote(mockUserEmail, mockDto).body!!

        // act
        voteScheduler.voteStatusTask()

        // assert
        voteRepository.findById(ObjectId(id)).get().apply {
            Assertions.assertThat(status).isEqualTo(BallotStatus.REGISTERED)
        }
    }

    @Test
    fun is_voteStatusTask_works_well_when_before_the_endTime() {
        // arrange
        registerMockUser()
        val now: Date = Date()
        val startTime = Date(now.time - 100)
        val finishTime = Date(now.time + 1000000000)
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy(startTime = startTime, finishTime = finishTime)
        val id: String = voteService.registerVote(mockUserEmail, mockDto).body!!

        // act
        voteScheduler.voteStatusTask()
        voteRepository.findById(ObjectId(id)).get().apply {
            Assertions.assertThat(status).isEqualTo(BallotStatus.OPEN)
        }
        voteScheduler.voteStatusTask()

        // assert: can't close Ballot now
        voteRepository.findById(ObjectId(id)).get().apply {
            Assertions.assertThat(status).isEqualTo(BallotStatus.OPEN)
        }
    }
}
