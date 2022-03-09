package com.duckbox.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@EnableSwagger2
@Configuration
class SwaggerConfig {
    private val API_NAME = "DuckBox API"
    private val API_VERSION = "0.0.1"
    private val API_DESCRIPTION = "DuckBox API Description"

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.any())
            .build()
            .apiInfo(apiInfo())
    }

    fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
            .title(API_NAME)
            .version(API_VERSION)
            .description(API_DESCRIPTION)
            .build()
    }
}
