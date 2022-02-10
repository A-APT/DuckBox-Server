package com.duckbox.service.ethereum

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String

@Service
class DIdService(private val ethereumService: EthereumService) {

    @Value("\${contract.address.did}")
    private lateinit var contractAddress: String

    private final val REGISTER = "registerId"
    private final val UNREGISTER = "removeId"
    private final val GET = "getId"

    fun registerDid(did: String): Boolean? {
        val inputParams = listOf<Type<*>>(Utf8String(did))
        val outputParams = listOf<TypeReference<*>>(object: TypeReference<Bool>() {})
        return ethereumService.ethSend(contractAddress, REGISTER, inputParams, outputParams) as Boolean?
    }

    fun removeDid(did: String) {
        val inputParams = listOf<Type<*>>(Utf8String(did))
        val outputParams = listOf<TypeReference<*>>()
        ethereumService.ethSend(contractAddress, UNREGISTER, inputParams, outputParams)
    }

    fun getDid(did: String): String? {
        val inputParams = listOf<Type<*>>(Utf8String(did))
        val outputParams = listOf<TypeReference<*>>(object: TypeReference<Utf8String>() {})
        return ethereumService.ethCall(contractAddress, GET, inputParams, outputParams) as String?
    }

    fun getOwner(): String? {
        val inputParams = listOf<Type<*>>()
        val outputParams = listOf<TypeReference<*>>(object: TypeReference<Address>() {})
        return ethereumService.ethCall(contractAddress, "owner", inputParams, outputParams) as String?
    }
}
