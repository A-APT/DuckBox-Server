package com.duckbox.security

import com.duckbox.dto.JWTToken
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*

@Component
class JWTTokenProvider(private val userDetailsService: CustomUserDetailsService) {
    @Value("\${jwt.signing.key}")
    private lateinit var SIGNING_KEY: String

    private val tokenPeriod: Long = 1000L * 60L * 10L // 10 minute
    private val refreshTokenPeriod: Long = 1000L * 60L * 60L * 24L * 30L * 3L // 3 weeks

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

    fun refreshToken(refreshToken: String?): JWTToken {
        if (refreshToken != null && verifyToken(refreshToken)) {
            val claims: Claims = getAllClaimsFromToken(refreshToken)
            val userPK: String = claims.subject
            val roles: List<String> = claims["roles"] as List<String>

            return generateToken(userPK, roles);
        }
        // TODO Fix Exception
        throw RuntimeException()
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

    fun getJwtTokenFromHeader(headers: HttpHeaders): String {
        val jwtToken: String? = headers["Authorization"]?.get(0)

        // Substring Authorization Since the value is in "Authorization":"Bearer JWT_TOKEN" format
        if(jwtToken != null && jwtToken.startsWith("Bearer ")) {
            return jwtToken.substring(7)
        }
        // TODO Fix Exception
        throw RuntimeException()
    }
}
