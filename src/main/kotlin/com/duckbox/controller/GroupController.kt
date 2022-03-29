package com.duckbox.controller

import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.group.GroupUpdateDto
import com.duckbox.security.JWTTokenProvider
import com.duckbox.service.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
class GroupController (
    private val groupService: GroupService,
    private val jwtTokenProvider: JWTTokenProvider
) {

    @GetMapping("/api/v1/group")
    fun getAllGroup(@RequestHeader httpHeaders: Map<String, String>): ResponseEntity<List<GroupDetailDto>> {
        return groupService.getGroups()
    }

    @PostMapping("/api/v1/group")
    fun register(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupRegisterDto: GroupRegisterDto): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        groupService.registerGroup(userEmail, groupRegisterDto)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/v1/group/detail")
    fun updateGroup(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupUpdateDto: GroupUpdateDto): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        groupService.updateGroup(userEmail, groupUpdateDto)
        return ResponseEntity.noContent().build()
    }
}
