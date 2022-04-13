package com.duckbox.config

import BlindSecp256k1
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BlindSignatureConfig {
    @Bean
    fun blindSecp256k1(): BlindSecp256k1 {
        return BlindSecp256k1()
    }
}
