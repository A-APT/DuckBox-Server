package com.duckbox.controller

import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.service.VoteService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class VoteController (
    private val voteService: VoteService
) {

    @PostMapping("/api/v1/vote")
    fun register(@RequestHeader httpHeaders: Map<String, String>, @RequestBody voteRegisterDto: VoteRegisterDto): ResponseEntity<Unit> {
        voteService.registerVote(voteRegisterDto)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/vote/group/{groupId}")
    fun findVotesOfGroup(@RequestHeader httpHeaders: Map<String, String>, @PathVariable groupId: String): ResponseEntity<List<VoteDetailDto>> {
        return voteService.findVotesOfGroup(groupId)
    }

}
