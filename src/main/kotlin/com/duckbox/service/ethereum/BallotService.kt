package com.duckbox.service.ethereum

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger
import javax.xml.bind.DatatypeConverter
import kotlin.streams.toList

@Service
class BallotService(private val ethereumService: EthereumService) {

    @Value("\${contract.address.ballots}")
    private lateinit var contractAddress: String

    private final val REGISTER = "registerBallot"
    private final val OPEN = "open"
    private final val CLOSE = "close"
    private final val RESULT = "resultOfBallot"

    fun registerBallot(did: String,
                       ballotId: String,
                       publicKeyX: BigInteger,
                       publicKeyY: BigInteger,
                       candidateNames: List<String>,
                       isOfficial: Boolean,
                       startTime: Long, // milliseconds
                       endTime: Long // milliseconds
    ): Boolean? {
        val candidateList: List<Utf8String> = candidateNames.stream().map {
            Utf8String(it)
        }.toList()
        val candidateDynamicArray = DynamicArray(Utf8String::class.java, candidateList)
        val inputParams = listOf<Type<*>>(
            Bytes32(DatatypeConverter.parseHexBinary(did)),
            Uint256(publicKeyX),
            Uint256(publicKeyY),
            Utf8String(ballotId),
            candidateDynamicArray,
            Bool(isOfficial),
            Uint256(startTime),
            Uint256(endTime)
        )
        val outputParams = listOf<TypeReference<*>>(object: TypeReference<Bool>() {})
        return ethereumService.ethSendRaw(contractAddress, REGISTER, inputParams, outputParams) as Boolean?
    }

    fun open(ballotId: String) {
        val inputParams = listOf<Type<*>>(Utf8String(ballotId))
        val outputParams = listOf<TypeReference<*>>()
        ethereumService.ethSendRaw(contractAddress, OPEN, inputParams, outputParams)
    }

    fun close(ballotId: String, totalNum: Int) {
        val inputParams = listOf<Type<*>>(Utf8String(ballotId), Uint256(totalNum.toLong()))
        val outputParams = listOf<TypeReference<*>>()
        ethereumService.ethCall(contractAddress, CLOSE, inputParams, outputParams)
    }

    fun resultOfBallot(ballotId: String): List<BigInteger> {
        val inputParams = listOf<Type<*>>(Utf8String(ballotId))
        val outputParams = listOf<TypeReference<*>>(object: TypeReference<DynamicArray<Uint>>() {})
        val decoded: List<Type<*>> = ethereumService.ethCall(contractAddress, RESULT, inputParams, outputParams)!!
        val result: MutableList<BigInteger> = mutableListOf()
        (decoded[0].value as List<Uint>).forEach {
            result.add(it.value)
        }
        return result
    }
}
