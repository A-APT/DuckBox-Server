package com.duckbox.controller

import com.duckbox.dto.JWTToken
import com.duckbox.dto.user.*
import com.duckbox.service.UserService
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
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

    @PostMapping("/api/v1/user/refresh")
    fun refreshToken(@RequestBody refreshToken: String): ResponseEntity<JWTToken> {
        return userService.refreshToken(refreshToken)
    }

    @PostMapping("/api/v1/user/group")
    fun joinGroup(@RequestHeader httpHeaders: Map<String, String>, @RequestBody joinGroupRequestDto: JoinGroupRequestDto): ResponseEntity<Unit> {
        userService.joinGroup(joinGroupRequestDto.email, joinGroupRequestDto.groupId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/v1/user/vote")
    fun joinVote(@RequestHeader httpHeaders: Map<String, String>, @RequestBody joinVoteRequestDto: JoinVoteRequestDto): ResponseEntity<Unit> {
        userService.joinVote(joinVoteRequestDto.email, joinVoteRequestDto.voteId)
        return ResponseEntity.noContent().build()
    }

}
