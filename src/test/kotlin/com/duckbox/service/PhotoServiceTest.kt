package com.duckbox.service

import com.duckbox.domain.photo.PhotoRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.bson.types.Binary

@SpringBootTest
@ExtendWith(SpringExtension::class)
class PhotoServiceTest {

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var photoService: PhotoService

    @BeforeEach
    @AfterEach
    fun init() {
        photoRepository.deleteAll()
    }

    fun getTestMockMultipart(): MockMultipartFile {
        val uploadFileName: String = "test.txt"
        val uploadFileContent: ByteArray = "test file!".toByteArray()
        return MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )
    }

    @Test
    fun is_savePhoto_works_well_on_bytearray() {
        // act
        val byteArray = "test file!".toByteArray()
        val id = photoService.savePhoto(byteArray)

        // assert
        photoRepository.findById(id).apply {
            assertThat(isEmpty).isEqualTo(false)
            assertThat(get().id).isEqualTo(id)
            assertThat(get().image).isEqualTo(Binary(byteArray))
        }
    }

    @Test
    fun is_savePhoto_works_well_on_multipart_file() {
        // arrange
        val mockFile: MockMultipartFile = getTestMockMultipart()

        // act
        val id = photoService.savePhoto(mockFile)

        // assert
        photoRepository.findById(id).apply {
            assertThat(isEmpty).isEqualTo(false)
            assertThat(get().id).isEqualTo(id)
            assertThat(get().image).isEqualTo(Binary(mockFile.bytes))
        }
    }

    @Test
    fun is_getPhoto_works_well() {
        // arrange
        val mockFile: MockMultipartFile = getTestMockMultipart()
        val id = photoService.savePhoto(mockFile)

        // act
        val binary = photoService.getPhoto(id)

        // assert
        assertThat(binary).isEqualTo(Binary(mockFile.bytes))
    }

    @Test
    fun is_deletePhoto_works_well() {
        // arrange
        val mockFile: MockMultipartFile = getTestMockMultipart()
        val id = photoService.savePhoto(mockFile)

        // act
        photoService.deletePhoto(id)

        // assert
        photoRepository.findById(id).apply {
            assertThat(isEmpty).isEqualTo(true)
        }
    }
}
