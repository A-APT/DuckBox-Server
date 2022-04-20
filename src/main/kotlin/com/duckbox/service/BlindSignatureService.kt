package com.duckbox.service

import BlindSecp256k1
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Service
import java.math.BigInteger

@PropertySource("classpath:security.properties")
@Service
class BlindSignatureService (private val blindSecp256k1: BlindSecp256k1){

    @Value("\${blind.privateKey}")
    private lateinit var privateKeyValue: String

    @Value("\${blind.k}")
    private lateinit var kValue: String

    fun blindSig(blindMessage: BigInteger): BigInteger {
        val privateKey: BigInteger = BigInteger(privateKeyValue, 16)
        val k: BigInteger = BigInteger(kValue, 16)
        return blindSecp256k1.blindSign(privateKey, k, blindMessage)
    }

    fun blindSig(blindMessage: BigInteger, privateKey: BigInteger): BigInteger {
        val k: BigInteger = BigInteger(kValue, 16)
        return blindSecp256k1.blindSign(privateKey, k, blindMessage)
    }
}