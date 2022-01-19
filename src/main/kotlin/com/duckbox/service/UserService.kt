package com.duckbox.service

import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.JWTToken
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.LoginResponseDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.security.JWTTokenProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class UserService (
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JWTTokenProvider
    ) {

    fun generateUserDID(): String {
        return "did" // TODO generate did
    }

    fun register(registerDto: RegisterDto) {
        userRepository.save(
            User(
                did =  generateUserDID(),
                studentId = registerDto.studentId,
                name = registerDto.name,
                password = registerDto.password,
                email = registerDto.email,
                nickname = registerDto.nickname,
                college = registerDto.college,
                department = registerDto.department,
                roles = setOf("ROLE_USER")
            )
        )
    }

    fun login(loginRequestDto: LoginRequestDto): ResponseEntity<LoginResponseDto> {
        // Find user
        lateinit var user: User
        runCatching {
            userRepository.findByEmail(loginRequestDto.email)
        }.onSuccess { user = it }.onFailure { throw it }

        // Check password
        if (user.password != loginRequestDto.password) {
            throw Exception("Email or Password is wrong.")
        }

        // Generate JWT token
        val jwtToken: JWTToken = jwtTokenProvider.generateToken(user.email, user.roles.toList())
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                LoginResponseDto(token = jwtToken.token, refreshToken = jwtToken.refreshToken)
            )
    }
}
