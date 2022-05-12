package com.duckbox

import com.duckbox.domain.survey.Question
import com.duckbox.domain.survey.QuestionType
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.survey.SurveyRegisterDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.dto.vote.VoteRegisterDto
import java.util.*

object MockDto {

    val mockRegisterDto = RegisterDto(
        studentId = 2019333,
        name = "je",
        password = "test",
        email = "email@konkuk.ac.kr",
        phoneNumber = "01012341234",
        nickname = "duck",
        college = "ku",
        department = listOf("computer", "software"),
        fcmToken = "temp",
    )

    val mockGroupRegisterDto = GroupRegisterDto(
        name = "testingGroup",
        leader = "did",
        description = "testing !",
        profile = null,
        header = null
    )

    val mockVoteRegisterDto = VoteRegisterDto(
        title = "title",
        content = "content",
        isGroup = false,
        groupId = null,
        startTime = Date(),
        finishTime = Date(),
        images = listOf(),
        ownerPrivate = DefinedValue.ownerPrivate,
        candidates = listOf("a", "b"),
        voters = listOf(1, 2),
        reward = false,
        notice = false
    )

    val mockSurveyRegisterDto = SurveyRegisterDto(
        title = "title",
        content = "content",
        isGroup = false,
        groupId = null,
        startTime = Date(),
        finishTime = Date(),
        images = listOf(),
        ownerPrivate = DefinedValue.ownerPrivate,
        questions = listOf(
            Question(QuestionType.MULTI, "hi", listOf("A", "B")),
            Question(QuestionType.LIKERT, "bye", null)
        ),
        targets = listOf(1, 2),
        reward = false,
        notice = false
    )
}