package com.duckbox.config

import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.web3j.protocol.Web3j

@SpringBootTest
@ExtendWith(SpringExtension::class)
class EthereumClientConfigTest {

    @Autowired
    private lateinit var web3j: Web3j

    @Test
    fun is_web3j_works_well() {
        runCatching {
            web3j.web3ClientVersion().send()
        }.onSuccess {
            print(it.web3ClientVersion)
        }.onFailure {
            fail("Something went wrong with web3j. Please check client address first.")
        }
    }

}
