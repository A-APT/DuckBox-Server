package com.duckbox.controller

import com.duckbox.dto.OverallDetailDto
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

    @GetMapping("/api/v1/groups/all")
    fun getAllGroup(@RequestHeader httpHeaders: Map<String, String>): ResponseEntity<List<GroupDetailDto>> {
        return groupService.getGroups()
    }

    @GetMapping("/api/v1/groups")
    fun getGroupsOfUser(@RequestHeader httpHeaders: Map<String, String>): ResponseEntity<List<GroupDetailDto>> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        return groupService.findGroupsOfUser(userEmail)
    }

    @GetMapping("/api/v1/groups/{query}")
    fun searchGroup(@RequestHeader httpHeaders: Map<String, String>, @PathVariable query: String): ResponseEntity<List<GroupDetailDto>> {
        return groupService.searchGroup(query)
    }

    @PostMapping("/api/v1/group")
    fun register(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupRegisterDto: GroupRegisterDto): ResponseEntity<String> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        return groupService.registerGroup(userEmail, groupRegisterDto)
    }

    @PutMapping("/api/v1/group")
    fun updateGroup(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupUpdateDto: GroupUpdateDto): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        groupService.updateGroup(userEmail, groupUpdateDto)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/api/v1/group")
    fun removeGroup(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupId: String): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        groupService.removeGroup(userEmail, groupId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/group/{groupId}")
    fun findGroupById(@RequestHeader httpHeaders: Map<String, String>, @PathVariable groupId: String): ResponseEntity<GroupDetailDto> {
        return groupService.findGroupById(groupId)
    }

    @PostMapping("/api/v1/group/member")
    fun joinGroup(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupId: String): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        groupService.joinGroup(userEmail, groupId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/api/v1/group/member")
    fun leaveGroup(@RequestHeader httpHeaders: Map<String, String>, @RequestBody groupId: String): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        groupService.leaveGroup(userEmail, groupId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/groups/items")
    fun findGroupVoteAndSurveyOfUser(@RequestHeader httpHeaders: Map<String, String>): ResponseEntity<List<OverallDetailDto>> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        return groupService.findGroupVoteAndSurveyOfUser(userEmail)
    }

    /* for test notification */
    @GetMapping("/api/v1/group/member/notification")
    fun testNotification(@RequestHeader httpHeaders: Map<String, String>): ResponseEntity<Unit> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        groupService.testNotification(userEmail)
        return ResponseEntity.noContent().build()
    }
}
