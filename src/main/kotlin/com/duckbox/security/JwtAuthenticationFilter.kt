package com.duckbox.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class JwtAuthenticationFilter(private val jwtTokenProvider: JWTTokenProvider) :GenericFilterBean() {
    @Value("\${jwt.header.string}")
    lateinit var HEADER_STRING: String

    @Value("\${jwt.token.prefix}")
    lateinit var TOKEN_PREFIX: String

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        // Get token from header
        val jwtToken: String = getJwtTokenFromHeader(request as HttpServletRequest)

        // Check token is valid
        if (jwtTokenProvider.verifyToken(jwtToken)) {
            // Get user information
            val authentication: Authentication = jwtTokenProvider.getAuthentication(jwtToken)

            // Save new authentication to security context
            SecurityContextHolder.getContext().authentication = authentication
        }
        chain?.doFilter(request, response)
    }

    private fun getJwtTokenFromHeader(request: HttpServletRequest): String {
        val jwtToken: String? = request.getHeader(HEADER_STRING)

        // Substring Authorization Since the value is in "Authorization":"Bearer JWT_TOKEN" format
        if(jwtToken != null && jwtToken.startsWith(HEADER_STRING)) {
            return jwtToken.substring(HEADER_STRING.length).trim()
        }
        // TODO Fix Exception
        throw RuntimeException()
    }

}
