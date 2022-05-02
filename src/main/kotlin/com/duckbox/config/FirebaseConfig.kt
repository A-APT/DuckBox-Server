package com.duckbox.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.io.ClassPathResource
import javax.annotation.PostConstruct

@PropertySource("classpath:security.properties")
@Configuration
class FirebaseConfig {
    @Value("\${app.firebase-config-file}")
    private lateinit var configPath: String

    @PostConstruct
    fun initialize() {
        // Get credentials to authorize this Spring Boot App.
        runCatching {
            if (FirebaseApp.getApps().isEmpty()) {
                val options = FirebaseOptions.builder()
                    .setCredentials(
                        GoogleCredentials.fromStream(
                            ClassPathResource(configPath).inputStream
                        )
                    ).build()
                FirebaseApp.initializeApp(options)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
}