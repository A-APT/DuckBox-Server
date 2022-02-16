package com.duckbox.service

import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.dto.group.RegisterGroupDto
import com.duckbox.errors.exception.ConflictException
import org.springframework.stereotype.Service

@Service
class GroupService (private val groupRepository: GroupRepository){

    fun registerGroup(registerDto: RegisterGroupDto) {
        // check duplicate
        // Group name is unique, so...
        runCatching {
            groupRepository.findByName(registerDto.name)
        }.onSuccess {
            throw ConflictException("Group [${registerDto.name}] is already registered.")
        }

        // save to server
        groupRepository.save(
            GroupEntity(
                name = registerDto.name,
                leader = registerDto.leader,
                status = GroupStatus.PENDING,
                description = registerDto.description,
                menbers = 0,
                profile = registerDto.profile,
                header = registerDto.header
            )
        )
    }
}
