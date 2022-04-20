package com.duckbox.controller

import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.dto.vote.VoteToken
import com.duckbox.security.JWTTokenProvider
import com.duckbox.service.VoteService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class VoteController (
    private val voteService: VoteService,
    private val jwtTokenProvider: JWTTokenProvider,
) {

    @PostMapping("/api/v1/vote")
    fun register(@RequestHeader httpHeaders: Map<String, String>, @RequestBody voteRegisterDto: VoteRegisterDto): ResponseEntity<String> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        return voteService.registerVote(userEmail, voteRegisterDto)
    }

    @GetMapping("/api/v1/vote")
    fun getAllVote(@RequestHeader httpHeaders: Map<String, String>): ResponseEntity<List<VoteDetailDto>> {
        return voteService.getAllVote()
    }

    @GetMapping("/api/v1/vote/group/{groupId}")
    fun findVotesOfGroup(@RequestHeader httpHeaders: Map<String, String>, @PathVariable groupId: String): ResponseEntity<List<VoteDetailDto>> {
        return voteService.findVotesOfGroup(groupId)
    }

    @PostMapping("/api/v1/vote/my")
    fun generateVoteToken(@RequestHeader httpHeaders: Map<String, String>, @RequestBody blindSigRequestDto: BlingSigRequestDto): ResponseEntity<VoteToken> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        return voteService.generateBlindSigVoteToken(userEmail, blindSigRequestDto)
    }

}
