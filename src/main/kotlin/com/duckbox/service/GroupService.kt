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
    private val photoService: PhotoService,
    private val userService: UserService,
){

    fun getGroups(): ResponseEntity<List<GroupDetailDto>> {
        val groupDtoList: MutableList<GroupDetailDto> = mutableListOf()
        groupRepository.findAll().forEach {
            val profile: ByteArray? = if(it.profile != null) photoService.getPhoto(it.profile!!).data else null
            val header: ByteArray? = if(it.header != null) photoService.getPhoto(it.header!!).data else null
            groupDtoList.add(it.toGroupDetailDto(profile, header))
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                groupDtoList
            )
    }

    fun registerGroup(userEmail: String, registerDto: GroupRegisterDto): ResponseEntity<String> {
        // check did is correct
        userService.checkValidUser(userEmail, registerDto.leader)

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
        val id: ObjectId = groupRepository.save(
            GroupEntity(
                name = registerDto.name,
                leader = registerDto.leader,
                status = GroupStatus.PENDING,
                description = registerDto.description,
                profile = profileImageId,
                header = headerImageId
            )
        ).id

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                id.toString()
            )
    }

    fun updateGroup(userEmail: String, groupUpdateDto: GroupUpdateDto): GroupEntity {
        // check group is valid
        lateinit var groupEntity: GroupEntity
        runCatching {
            groupRepository.findById(ObjectId(groupUpdateDto.id)).get()
        }.onSuccess {
            groupEntity = it
        }.onFailure {
            throw NotFoundException("Invalid GroupId: [${groupUpdateDto.id}]")
        }

        // check did is correct
        userService.checkValidUser(userEmail, groupEntity.leader)

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

    fun searchGroup(query: String): ResponseEntity<List<GroupDetailDto>> {
        val groupDtoList: MutableList<GroupDetailDto> = mutableListOf()
        groupRepository.findByNameContains(query).forEach {
            val profile: ByteArray? = if(it.profile != null) photoService.getPhoto(it.profile!!).data else null
            val header: ByteArray? = if(it.header != null) photoService.getPhoto(it.header!!).data else null
            groupDtoList.add(it.toGroupDetailDto(profile, header))
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                groupDtoList
            )
    }
}
