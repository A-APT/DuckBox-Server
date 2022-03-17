package com.duckbox.service

import com.duckbox.MockDto
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

@SpringBootTest
@ExtendWith(SpringExtension::class)
class VoteServiceTest {
    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var voteService: VoteService

    private val mockVoteRegisterDto: VoteRegisterDto = MockDto.mockVoteRegisterDto

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
        val mockDto: VoteRegisterDto = mockVoteRegisterDto.copy()
        mockDto.images = listOf("test file!".toByteArray())

        // act
        val id = voteService.registerVote(mockDto)

        // assert
        voteRepository.findById(id).get().apply {
            assertThat(title).isEqualTo(mockDto.title)
            assertThat(content).isEqualTo(mockDto.content)
            assertThat(isGroup).isEqualTo(mockDto.isGroup)
            assertThat(images.size).isEqualTo(1)
            assertThat(photoRepository.findById(images[0]).isPresent).isEqualTo(true)
        }
    }

}
