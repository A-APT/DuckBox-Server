package com.duckbox.service

import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.JWTToken
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.LoginResponseDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.security.JWTTokenProvider
import com.duckbox.service.ethereum.DIdService
import com.duckbox.utils.HashUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class UserService (
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JWTTokenProvider,
    private val hashUtils: HashUtils,
    private val didService: DIdService
    ) {

    fun generateUserDID(targetEmail: String, targetNumber: String): String {
        // generate did using SHA-256: current time + email + phone-number
        val didInfo = "${System.currentTimeMillis()}${targetEmail}${targetNumber}"
        return hashUtils.SHA256(didInfo)
    }

    fun register(registerDto: RegisterDto) {
        // TODO duplicate
        // send transaction
        val did = generateUserDID(registerDto.email, registerDto.phoneNumber)
        didService.registerDid(did)

        // save to server
        userRepository.save(
            User(
                did = did,
                studentId = registerDto.studentId,
                name = registerDto.name,
                password = registerDto.password,
                email = registerDto.email,
                phoneNumber = registerDto.phoneNumber,
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
