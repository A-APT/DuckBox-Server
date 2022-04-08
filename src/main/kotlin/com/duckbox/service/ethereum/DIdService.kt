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

    fun registerDid(address: String, did: String): Boolean? {
        val inputParams = listOf<Type<*>>(Address(address), Utf8String(did))
        val outputParams = listOf<TypeReference<*>>(object: TypeReference<Bool>() {})
        return ethereumService.ethSendRaw(contractAddress, REGISTER, inputParams, outputParams) as Boolean?
    }

    fun removeDid(address: String) {
        val inputParams = listOf<Type<*>>(Address(address))
        val outputParams = listOf<TypeReference<*>>()
        ethereumService.ethSendRaw(contractAddress, UNREGISTER, inputParams, outputParams)
    }

    fun getOwner(): String? {
        val inputParams = listOf<Type<*>>()
        val outputParams = listOf<TypeReference<*>>(object: TypeReference<Address>() {})
        return ethereumService.ethCall(contractAddress, "owner", inputParams, outputParams) as String?
    }
}
