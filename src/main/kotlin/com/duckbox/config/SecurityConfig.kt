package com.duckbox.config

import com.duckbox.security.JWTTokenProvider
import com.duckbox.security.JwtAuthenticationFilter
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
                .antMatchers("/api/v1/group/**",
                                        "/api/v1/groups/**",
                                        "/api/v1/vote/**",
                                        "/api/v1/survey/**").hasRole("USER")
                .antMatchers("/**").permitAll()
            .and()
                .addFilterBefore(
                    JwtAuthenticationFilter(jwtTokenProvider),
                    UsernamePasswordAuthenticationFilter::class.java
                )
    }
}
