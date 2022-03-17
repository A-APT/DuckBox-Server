package com.duckbox.controller

import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.service.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController


@RestController
class GroupController (
    private val groupService: GroupService,
) {

    @PostMapping("/api/v1/group/register")
    fun register(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupRegisterDto: GroupRegisterDto): ResponseEntity<Unit> {
        groupService.registerGroup(groupRegisterDto)
        return ResponseEntity.noContent().build()
    }

}
