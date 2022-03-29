package com.duckbox.service

import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.group.GroupUpdateDto
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.NotFoundException
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class GroupService (
    private val groupRepository: GroupRepository,
    private val photoService: PhotoService
){

    fun getGroups(): ResponseEntity<List<GroupDetailDto>> {
        val groupDtoList: MutableList<GroupDetailDto> = mutableListOf()
        groupRepository.findAll().forEach {
            groupDtoList.add(it.toGroupDetailDto())
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                groupDtoList
            )
    }

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
                profile = profileImageId,
                header = headerImageId
            )
        ).id
    }

    fun updateGroup(groupUpdateDto: GroupUpdateDto): GroupEntity {
        lateinit var groupEntity: GroupEntity
        runCatching {
            groupRepository.findById(ObjectId(groupUpdateDto.id)).get()
        }.onSuccess {
            groupEntity = it
        }.onFailure {
            throw NotFoundException("Group [${groupUpdateDto.id}] was not registered.")
        }

        groupUpdateDto.apply {
            if (description != null) groupEntity.description = description!!
            if (profile != null) { // profile image has changed
                if(groupEntity.profile != null) photoService.deletePhoto(groupEntity.profile!!) // delete original
                groupEntity.profile = photoService.savePhoto(profile!!) // save changed
            }
            if (header != null) { // header image has changed
                if(groupEntity.header != null) photoService.deletePhoto(groupEntity.header!!) // delete original
                groupEntity.header = photoService.savePhoto(header!!) // save changed
            }
        }

        // update to server
        return groupRepository.save(groupEntity)
    }
}
