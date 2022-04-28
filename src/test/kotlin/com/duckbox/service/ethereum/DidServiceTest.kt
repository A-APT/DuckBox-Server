package com.duckbox.service.ethereum

import com.duckbox.errors.exception.EthereumException
import com.duckbox.utils.HashUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.springframework.beans.factory.annotation.Value

//@SpringBootTest
@ExtendWith(SpringExtension::class)
class DidServiceTest {
    @Autowired
    private lateinit var didService: DIdService

    @Value("\${contract.owner}")
    private lateinit var ownerAddress: String

    @Autowired
    private lateinit var hashUtils: HashUtils

    private val did = "did.test2"

    //@Test
    fun is_didService_works_well() {
        val hash: String = hashUtils.SHA256(did)
        didService.registerDid(address = ownerAddress, did = hash)
        runCatching {
            didService.registerDid(address = ownerAddress, did = hash)
        }.onSuccess {
            fail("This should be failed.")
        }.onFailure {
            assertThat(it is EthereumException).isEqualTo(true)
        }
        didService.removeDid(address = ownerAddress)
    }

    //@Test
    fun is_getOwner_works_well() {
        didService.getOwner().apply {
            assertThat(this).isEqualTo(ownerAddress)
        }
    }
}
