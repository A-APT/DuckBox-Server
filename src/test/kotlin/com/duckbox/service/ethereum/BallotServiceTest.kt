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
import java.math.BigInteger
import java.util.*

//@SpringBootTest
@ExtendWith(SpringExtension::class)
class BallotServiceTest {
    @Autowired
    private lateinit var ballotService: BallotService

    @Autowired
    private lateinit var hashUtils: HashUtils

    private val did = "did.test"
    private val ballotId = "ballot2"

    //@Test
    fun is_registerBallot_works_well() {
        val hash: String = hashUtils.SHA256(did)
        ballotService.registerBallot(
            did = hash,
            ballotId = ballotId,
            publicKeyX = BigInteger("4719ded852f84728c0e25e2a7111e880f4ef516155f62e3db82be7b2981b0323", 16),
            publicKeyY = BigInteger("84813d29f2125b707bc94244aec3c3d52a8025b5f7c988c92736daa22a621ac", 16),
            candidateNames = listOf("c1", "c2"),
            isOfficial = false,
            startTime = Date().time,
            endTime = Date().time + 100
        )
    }

    //@Test
    fun is_open_ballot_works_well() {
        ballotService.open(ballotId)
    }

    //Test
    fun is_close_ballot_works_well() {
        ballotService.close(ballotId, 0)
    }
}
