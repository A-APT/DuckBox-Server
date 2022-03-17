package com.duckbox

import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.vote.VoteRegisterDto
import java.util.*

object MockUser {
    val mockRegisterGroupDto = GroupRegisterDto(
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
        candidates = listOf("a", "b"),
        voters = listOf(1, 2),
        reward = false,
        notice = false
    )
}