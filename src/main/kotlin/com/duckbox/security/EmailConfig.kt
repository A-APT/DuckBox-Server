package com.duckbox.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

@PropertySource("classpath:security.properties")
@Configuration
class EmailConfig {
    @Value("\${mail.admin.id}")
    private lateinit var id: String

    @Value("\${mail.admin.password}")
    private lateinit var password: String

    @Bean
    fun mailService(): JavaMailSender {
        val mailSender: JavaMailSenderImpl = JavaMailSenderImpl()
        mailSender.host = "smtp.gmail.com"
        mailSender.port = 587
        mailSender.username = id
        mailSender.password = password
        mailSender.protocol = "smtp"
        configureMailProperties(mailSender.javaMailProperties)
        return mailSender
    }

    private fun configureMailProperties(properties: Properties) {
        properties["mail.transport.protocol"] = "smtp"
        properties["mail.smtp.auth"] = true
        properties["mail.smtp.starttls.enable"] = true
        properties["mail.smtp.starttls.required"] = true
    }
}
