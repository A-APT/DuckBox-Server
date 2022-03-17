package com.duckbox.service

import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.errors.exception.ConflictException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class GroupService (
    private val groupRepository: GroupRepository,
    private val photoService: PhotoService
){

    fun registerGroup(registerDto: GroupRegisterDto): ObjectId {
        // check duplicate
        // Group name is unique, so...
        runCatching {
            groupRepository.findByName(registerDto.name)
        }.onSuccess {
            throw ConflictException("Group [${registerDto.name}] is already registered.")
        }

        var profileImageId: ObjectId? = null
        var headerImageId: ObjectId? = null
        registerDto.apply {
            profileImageId = if (profile != null) photoService.savePhoto(profile!!) else null
            headerImageId = if (header != null) photoService.savePhoto(header!!) else null
        }

        // save to server
        return groupRepository.save(
            GroupEntity(
                name = registerDto.name,
                leader = registerDto.leader,
                status = GroupStatus.PENDING,
                description = registerDto.description,
                menbers = 0,
                profile = profileImageId,
                header = headerImageId
            )
        ).id
    }
}
