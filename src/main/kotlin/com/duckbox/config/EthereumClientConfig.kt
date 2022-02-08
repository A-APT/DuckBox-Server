package com.duckbox.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.web3j.protocol.Web3j
import org.web3j.protocol.admin.Admin
import org.web3j.protocol.http.HttpService

@PropertySource("classpath:ethereum.properties")
@Configuration
class EthereumClientConfig {
    @Value("\${web3j.client-address}")
    private lateinit var clientAddress: String

    @Value("\${contract.owner}")
    private lateinit var owner: String

    @Value("\${contract.owner-private}")
    private lateinit var ownerPrivate: String

    @Bean
    fun web3j(): Web3j {
        // unlock owner's account
        Admin.build(HttpService(clientAddress))
            .personalUnlockAccount(owner, ownerPrivate)
        return Web3j.build(HttpService(clientAddress))
    }

    @Bean
    fun adminWeb3j(): Admin {
        return Admin.build(HttpService(clientAddress))
    }
}
