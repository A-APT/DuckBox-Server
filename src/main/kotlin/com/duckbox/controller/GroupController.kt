package com.duckbox.controller

import com.duckbox.dto.group.RegisterGroupDto
import com.duckbox.security.JWTTokenProvider
import com.duckbox.service.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController


@RestController
class GroupController (
    private val groupService: GroupService,
    private val jwtTokenProvider: JWTTokenProvider
) {

    @PostMapping("/api/v1/group/register")
    fun register(@RequestHeader httpHeaders: Map<String, String>, registerGroupDto: RegisterGroupDto): ResponseEntity<Unit> {
        val token = jwtTokenProvider.getTokenFromHeader(httpHeaders)
        groupService.registerGroup(registerGroupDto)
        return ResponseEntity.noContent().build()
    }

}
