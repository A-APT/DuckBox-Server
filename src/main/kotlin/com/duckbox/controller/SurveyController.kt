package com.duckbox.controller

import com.duckbox.dto.survey.SurveyDetailDto
import com.duckbox.dto.survey.SurveyRegisterDto
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

    @GetMapping("/api/v1/survey/group/{groupId}")
    fun findSurveysOfGroup(@RequestHeader httpHeaders: Map<String, String>, @PathVariable groupId: String): ResponseEntity<List<SurveyDetailDto>> {
        return surveyService.findSurveysOfGroup(groupId)
    }
}