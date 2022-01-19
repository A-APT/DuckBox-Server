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

    val HEADER_STRING: String = "Authorization"
    val TOKEN_PREFIX: String = "Bearer"

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        // Get token from header
        val jwtToken: String? = getTokenFromHeader(request as HttpServletRequest)

        // Check token is valid
        if (jwtToken != null && jwtTokenProvider.verifyToken(jwtToken)) {
            // Get user information
            val authentication: Authentication = jwtTokenProvider.getAuthentication(jwtToken)

            // Save new authentication to security context
            SecurityContextHolder.getContext().authentication = authentication
        }
        chain?.doFilter(request, response)
    }

    private fun getTokenFromHeader(request: HttpServletRequest): String? {
        val token: String? = request.getHeader(HEADER_STRING)

        // Substring Authorization Since the value is in "Authorization":"Bearer JWT_TOKEN" format
        return if(token != null && token.startsWith(TOKEN_PREFIX)) {
            token.substring(TOKEN_PREFIX.length).trim()
        } else null
    }
}
