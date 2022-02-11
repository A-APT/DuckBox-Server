package com.duckbox.service.ethereum

import com.duckbox.errors.exception.EthereumException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail

@SpringBootTest
@ExtendWith(SpringExtension::class)
class DidServiceTest {
    @Autowired
    private lateinit var didService: DIdService

    private val did = "did.test2"

    @Test
    fun is_registerDId_works_well() {
        // arrange
        didService.registerDid(did = did)

        // act & assert
        didService.getDid(did).apply { // TODO
            assertThat(this!!).isEqualTo(did)
        }

        // clean up
        didService.removeDid(did = did)
    }

    @Test
    fun is_registerDId_works_on_duplicate() {
        didService.registerDid(did = did)
        runCatching {
            didService.registerDid(did = did)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is EthereumException).isEqualTo(true)
        }

        // clean up
        didService.removeDid(did = did)
    }

    @Test
    fun is_removeDid_works_well() {
        // arrange
        didService.registerDid(did = did)

        // act
        didService.removeDid(did = did)

        // assert
        runCatching {
            didService.getDid(did)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is EthereumException).isEqualTo(true)
        }
    }

    @Test
    fun is_getOwner_works_well() {
        didService.getOwner().apply {
            println(this)
        }
    }
}
