package com.duckbox.service

import com.duckbox.domain.photo.PhotoRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.vote.VoteRegisterDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.mock.web.MockMultipartFile
import java.util.*

@SpringBootTest
@ExtendWith(SpringExtension::class)
class VoteServiceTest {
    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var voteService: VoteService

    private val mockVoteRegisterDto = VoteRegisterDto(
        title = "title",
        content = "content",
        isGroup = false,
        groupId = null,
        startTime = Date(),
        finishTime = Date(),
        images = listOf(),
        candidates = listOf("a", "b"),
        voters = listOf(1, 2),
        reward = false,
        notice = false
    )

    @BeforeEach
    @AfterEach
    fun init() {
        voteRepository.deleteAll()
        photoRepository.deleteAll()
    }

    @Test
    fun is_registerVote_works_well() {
        // act
        val id = voteService.registerVote(mockVoteRegisterDto)

        // assert
        voteRepository.findById(id).get().apply {
            assertThat(title).isEqualTo(mockVoteRegisterDto.title)
            assertThat(content).isEqualTo(mockVoteRegisterDto.content)
            assertThat(isGroup).isEqualTo(mockVoteRegisterDto.isGroup)
            assertThat(images.size).isEqualTo(0)
        }
    }

    @Test
    fun is_registerVote_works_multipartFile() {
        // arrange
        val uploadFileName: String = "test.txt"
        val uploadFileContent: ByteArray = "test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )
        mockVoteRegisterDto.images = listOf(multipartFile)

        // act
        val id = voteService.registerVote(mockVoteRegisterDto)

        // assert
        voteRepository.findById(id).get().apply {
            assertThat(title).isEqualTo(mockVoteRegisterDto.title)
            assertThat(content).isEqualTo(mockVoteRegisterDto.content)
            assertThat(isGroup).isEqualTo(mockVoteRegisterDto.isGroup)
            assertThat(images.size).isEqualTo(1)
            assertThat(photoRepository.findById(images[0]).isPresent).isEqualTo(true)
        }
    }

}
