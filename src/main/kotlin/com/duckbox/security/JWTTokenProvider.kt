package com.duckbox.security

import com.duckbox.dto.JWTToken
import com.duckbox.errors.exception.UnauthorizedException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*

@PropertySource("classpath:security.properties")
@Component
class JWTTokenProvider(private val userDetailsService: CustomUserDetailsService) {
    @Value("\${jwt.signing.key}")
    private lateinit var SIGNING_KEY: String

    val HEADER_STRING: String = "authorization" // for HttpHeaders (Authorization)
    val TOKEN_PREFIX: String = "Bearer"

    private val tokenPeriod: Long = 1000L * 60L * 60L * 24L * 7L // 1 week
    private val refreshTokenPeriod: Long = 1000L * 60L * 60L * 24L * 7L * 3L // 3 weeks

    fun generateToken(userPK: String, roles: List<String>): JWTToken {
        val claims: Claims = Jwts.claims().setSubject(userPK).apply {
            put("roles", roles)
        }

        val date: Date = Date()

        return JWTToken(
            token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(date)
                .setExpiration(Date(date.time + tokenPeriod))
                .signWith(SignatureAlgorithm.HS256, SIGNING_KEY)
                .compact(),
            refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(date)
                .setExpiration(Date(date.time + refreshTokenPeriod))
                .signWith(SignatureAlgorithm.HS256, SIGNING_KEY)
                .compact()
        )
    }

    fun verifyToken(jwtToken: String): Boolean {
        return runCatching {
            val expiration: Date = getAllClaimsFromToken(jwtToken).expiration
            !expiration.before(Date())
        }.getOrDefault(false)
    }

    fun refreshToken(refreshToken: String): JWTToken {
        if (verifyToken(refreshToken)) {
            val claims: Claims = getAllClaimsFromToken(refreshToken)
            val userPK: String = claims.subject
            val roles: List<String> = claims["roles"] as List<String>

            return generateToken(userPK, roles);
        }
        throw UnauthorizedException("Failed when refresh token.")
    }

    fun getAuthentication(jwtToken: String): Authentication {
        val userDetails: UserDetails = userDetailsService.loadUserByUsername(getUserPK(jwtToken))
        return UsernamePasswordAuthenticationToken(
            userDetails, "", userDetails.authorities
        )
    }

    fun getAllClaimsFromToken(jwtToken: String): Claims {
        return Jwts.parser()
            .setSigningKey(SIGNING_KEY)
            .parseClaimsJws(jwtToken)
            .body
    }

    fun getUserPK(jwtToken: String): String {
        return getAllClaimsFromToken(jwtToken).subject
    }

    fun getTokenFromHeader(headers: Map<String, String>): String? {
        var token: String = ""
        runCatching {
            token = headers[HEADER_STRING]!!
        }.onFailure {
            throw UnauthorizedException("JWT Token must included in header $HEADER_STRING")
        }
        // Substring Authorization Since the value is in "Authorization":"Bearer JWT_TOKEN" format
        if (token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length).trim()
        } else {
            throw UnauthorizedException("JWT Token must included in header $HEADER_STRING")
        }
    }
}
