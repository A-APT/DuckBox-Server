package com.duckbox.controller

import com.duckbox.dto.JWTToken
import com.duckbox.dto.user.*
import com.duckbox.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
    private val userService: UserService,
) {

    @PostMapping("/api/v1/user/register")
    fun register(@RequestBody registerDto: RegisterDto): ResponseEntity<String> {
        return userService.register(registerDto)
    }

    @PostMapping("/api/v1/user/login")
    fun login(@RequestBody loginRequestDto: LoginRequestDto): ResponseEntity<LoginResponseDto> {
        return userService.login(loginRequestDto)
    }

    @PostMapping("/api/v1/user/refresh")
    fun refreshToken(@RequestBody refreshToken: String): ResponseEntity<JWTToken> {
        return userService.refreshToken(refreshToken)
    }
}
