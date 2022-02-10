package com.duckbox.service.ethereum

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

@PropertySource("classpath:ethereum.properties")
@Service
class EthereumService(private val web3j: Web3j) {

    @Value("\${contract.owner}")
    private lateinit var ownerAddress: String

    @Value("\${contract.owner-private}")
    private lateinit var ownerPrivate: String


    fun ethCall(contractAddress: String, functionName: String, inputParams: List<Type<*>>, outputParams: List<TypeReference<*>>): Any? {
        // generate function
        val function = org.web3j.abi.datatypes.Function(functionName, inputParams, outputParams)
        val encodedFunction = FunctionEncoder.encode(function)

        // call function
        // createFunctionCallTransaction BigInteger
        val transaction = Transaction.createEthCallTransaction(ownerAddress, contractAddress, encodedFunction)
        val ethCall: EthCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get()

        if (ethCall.hasError()){
            throw Exception(ethCall.error.message) // TODO error handling
        }

        // decode response
        val decode = FunctionReturnDecoder.decode(ethCall.result, function.outputParameters)
        //print("ethcCall result ${ethCall.result} / value: ${decode[0].value} / type: ${decode[0].typeAsString}")
        return if (decode.size > 0) decode[0].value else null
    }

    fun ethSend(contractAddress: String, functionName: String, inputParams: List<Type<*>>, outputParams: List<TypeReference<*>>): Any? {
        // generate function
        val function = org.web3j.abi.datatypes.Function(functionName, inputParams, outputParams)
        val encodedFunction = FunctionEncoder.encode(function)

        // create raw transaction (:signed transaction)
        // Convert.toWei("1", Convert.Unit.GWEI).toBigInteger(), // gasPrice
        val transaction = Transaction.createEthCallTransaction(ownerAddress, contractAddress, encodedFunction)
        val ethSend: EthSendTransaction = web3j.ethSendTransaction(transaction).sendAsync().get()

        if (ethSend.hasError()){
            throw Exception(ethSend.error.message) // TODO error handling
        }

        // decode response
        val decode = FunctionReturnDecoder.decode(ethSend.result, function.outputParameters)
        return if (decode.size > 0) decode[0].value else null
    }

    fun ethSendRaw(contractAddress: String, functionName: String, inputParams: List<Type<*>>, outputParams: List<TypeReference<*>>): Any? {
        // generate function
        val function = org.web3j.abi.datatypes.Function(functionName, inputParams, outputParams)
        val encodedFunction = FunctionEncoder.encode(function)

        // send raw transaction
        val credentials: Credentials = Credentials.create(ownerPrivate)
        val manager = RawTransactionManager(web3j, credentials)
        val ethSend: EthSendTransaction = manager.sendTransaction(
            DefaultGasProvider.GAS_PRICE, // gasPrice
            BigInteger.valueOf(8000000), // gasLimit (ropsten)
            contractAddress, // to
            encodedFunction, // data
            BigInteger.ONE // value
        )

        if (ethSend.hasError()){
            throw Exception(ethSend.error.message) // TODO error handling
        }

//        val processor = PollingTransactionReceiptProcessor(
//            web3j,
//            TransactionManager.DEFAULT_POLLING_FREQUENCY,
//            TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH)
//        val receipt = processor.waitForTransactionReceipt(ethSend.transactionHash)

        // decode response
        val decode = FunctionReturnDecoder.decode(ethSend.result, function.outputParameters)
        return if (decode.size > 0) decode[0].value else null
    }

    fun getReceipt(transactionHash: String): TransactionReceipt? {
        val transactionReceipt: EthGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send()
        if (transactionReceipt.transactionReceipt.isPresent) {
            return transactionReceipt.result
        } else {
            return null
        }
    }
}