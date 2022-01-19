package com.duckbox.controller

import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.LoginResponseDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(private val userService: UserService) {

    @PostMapping("/api/v1/user/register")
    fun register(@RequestBody registerDto: RegisterDto): ResponseEntity<Unit> {
        userService.register(registerDto)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/v1/user/login")
    fun login(@RequestBody loginRequestDto: LoginRequestDto): ResponseEntity<LoginResponseDto> {
        return userService.login(loginRequestDto)
    }
}
