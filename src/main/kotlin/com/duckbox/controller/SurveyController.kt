package com.duckbox.controller

import com.duckbox.dto.BlindSigToken
import com.duckbox.dto.survey.SurveyDetailDto
import com.duckbox.dto.survey.SurveyRegisterDto
import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.security.JWTTokenProvider
import com.duckbox.service.SurveyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SurveyController (
    private val surveyService: SurveyService,
    private val jwtTokenProvider: JWTTokenProvider,
){
    @PostMapping("/api/v1/survey")
    fun register(@RequestHeader httpHeaders: Map<String, String>, @RequestBody surveyRegisterDto: SurveyRegisterDto): ResponseEntity<String> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        return surveyService.registerSurvey(userEmail, surveyRegisterDto)
    }

    @GetMapping("/api/v1/survey")
    fun getAllSurvey(@RequestHeader httpHeaders: Map<String, String>): ResponseEntity<List<SurveyDetailDto>> {
        return surveyService.getAllSurvey()
    }

    @GetMapping("/api/v1/survey/{surveyId}")
    fun findSurveyById(@RequestHeader httpHeaders: Map<String, String>, @PathVariable surveyId: String): ResponseEntity<SurveyDetailDto> {
        return surveyService.findSurveyById(surveyId)
    }

    @GetMapping("/api/v1/survey/group/{groupId}")
    fun findSurveysOfGroup(@RequestHeader httpHeaders: Map<String, String>, @PathVariable groupId: String): ResponseEntity<List<SurveyDetailDto>> {
        return surveyService.findSurveysOfGroup(groupId)
    }

    @PostMapping("/api/v1/survey/signatures")
    fun generateSurveyToken(@RequestHeader httpHeaders: Map<String, String>, @RequestBody blindSigRequestDto: BlingSigRequestDto): ResponseEntity<BlindSigToken> {
        val userEmail: String = jwtTokenProvider.getUserPK(jwtTokenProvider.getTokenFromHeader(httpHeaders)!!)
        return surveyService.generateBlindSigSurveyToken(userEmail, blindSigRequestDto)
    }
}