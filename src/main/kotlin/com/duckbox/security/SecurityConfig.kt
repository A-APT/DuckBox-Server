package com.duckbox.security

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@EnableWebSecurity
class SecurityConfig(private val jwtTokenProvider: JWTTokenProvider): WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .headers().frameOptions().disable()
            .and()
                .authorizeRequests()
                .antMatchers().hasRole("USER")
                .antMatchers("/**").permitAll()
            .and()
                .addFilterBefore(
                    JwtAuthenticationFilter(jwtTokenProvider),
                    UsernamePasswordAuthenticationFilter::class.java
                )
    }
}
