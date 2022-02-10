package com.duckbox.service.ethereum

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

import org.web3j.protocol.Web3j

@SpringBootTest
@ExtendWith(SpringExtension::class)
class DidServiceTest {
    @Autowired
    private lateinit var didService: DIdService

    private val did = "did.test"

    @Test
    fun is_registerDId_works_well() {
        val response = didService.registerDid(did = did)
        println(response)
    }

    @Test
    fun is_removeDid_works_well() {
        // arrange
        val response = didService.removeDid(did = did)
        println(response)
    }

    @Test
    fun is_getDId_works_well() {
        didService.getDid(did).apply {
            println(this)
        }
    }

    @Test
    fun is_getOwner_works_well() {
        didService.getOwner().apply {
            println(this)
        }
    }
}
