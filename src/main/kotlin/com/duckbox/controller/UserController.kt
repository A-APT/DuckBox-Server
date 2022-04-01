package com.duckbox.controller

import com.duckbox.dto.JWTToken
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.user.*
import com.duckbox.security.JWTTokenProvider
import com.duckbox.service.UserService
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
    private val userService: UserService,
    private val jwtTokenProvider: JWTTokenProvider,
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

    @PostMapping("/api/v1/user/group")
    fun joinGroup(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupId: String): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        userService.joinGroup(userEmail, groupId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/v1/user/vote")
    fun joinVote(@RequestHeader httpHeaders: Map<String, String>, @RequestBody voteId: String): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        userService.joinVote(userEmail, voteId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/user/group")
    fun getGroupsByUser(@RequestHeader httpHeaders: Map<String, String>): ResponseEntity<List<GroupDetailDto>> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        return userService.findGroupsByUser(userEmail)
    }
}
