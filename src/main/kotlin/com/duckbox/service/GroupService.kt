package com.duckbox.service

import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.domain.survey.SurveyRepository
import com.duckbox.domain.user.UserBox
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.OverallDetailDto
import com.duckbox.dto.group.GroupDetailDto
import com.duckbox.dto.group.GroupRegisterDto
import com.duckbox.dto.group.GroupUpdateDto
import com.duckbox.dto.notification.NotificationMessage
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import com.google.firebase.messaging.FirebaseMessaging
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class GroupService (
    private val groupRepository: GroupRepository,
    private val photoService: PhotoService,
    private val userService: UserService,
    private val fcmService: FCMService,
    private val userRepository: UserRepository,
    private val userBoxRepository: UserBoxRepository,
    private val voteRepository: VoteRepository,
    private val surveyRepository: SurveyRepository,
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

    fun findGroupsOfUser(userEmail: String): ResponseEntity<List<GroupDetailDto>> {
        val groupIdList: MutableList<ObjectId> = userBoxRepository.findByEmail(userEmail).groups
        val groupDtoList: MutableList<GroupDetailDto> = mutableListOf()
        groupIdList.forEach {
            val groupEntity: GroupEntity = groupRepository.findById(it).get()
            val profile: ByteArray? = if(groupEntity.profile != null) photoService.getPhoto(groupEntity.profile!!).data else null
            val header: ByteArray? = if(groupEntity.header != null) photoService.getPhoto(groupEntity.header!!).data else null
            groupDtoList.add(groupEntity.toGroupDetailDto(profile, header))

        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                groupDtoList
            )
    }

    fun findGroupById(groupId: String): ResponseEntity<GroupDetailDto> {
        lateinit var groupEntity: GroupEntity
        runCatching {
            groupRepository.findById(ObjectId(groupId)).get()
        }.onSuccess {
            groupEntity = it
        }.onFailure {
            throw NotFoundException("Invalid GroupId: [${groupId}]")
        }
        val profile: ByteArray? = if(groupEntity.profile != null) photoService.getPhoto(groupEntity.profile!!).data else null
        val header: ByteArray? = if(groupEntity.header != null) photoService.getPhoto(groupEntity.header!!).data else null
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                groupEntity.toGroupDetailDto(profile, header)
            )
    }

    fun findGroupVoteAndSurveyOfUser(userEmail: String): ResponseEntity<List<OverallDetailDto>> {
        val userStudentId: Int = userRepository.findByEmail(userEmail).studentId
        val userBox: UserBox = userBoxRepository.findByEmail(userEmail)
        val detailDtoList: MutableList<OverallDetailDto> = mutableListOf()
        userBox.groups.forEach { groupId ->
            voteRepository.findAllByGroupId(groupId.toString()).forEach { vote ->
                if(!(vote.voters != null && vote.voters?.find { it == userStudentId } == null)) {
                    // have right to this vote
                    val images: MutableList<ByteArray> = mutableListOf()
                    vote.images.forEach { photoId ->
                        images.add(photoService.getPhoto(photoId).data)
                    }
                    detailDtoList.add(vote.toOverallDetailDto(
                        _images = images,
                        _isAvailable = userBox.votes.find { it == vote.id } == null
                    ))
                }
            }
            surveyRepository.findAllByGroupId(groupId.toString()).forEach { survey ->
                if(!(survey.targets != null && survey.targets?.find { it == userStudentId } == null)) {
                    // have right to this survey
                    val images: MutableList<ByteArray> = mutableListOf()
                    survey.images.forEach { photoId ->
                        images.add(photoService.getPhoto(photoId).data)
                    }
                    detailDtoList.add(survey.toOverallDetailDto(
                        _images = images,
                        _isAvailable = userBox.surveys.find { it == survey.id } == null
                    ))
                }
            }
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                detailDtoList
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

        // add to owner's box
        userBoxRepository.findByEmail(userEmail).apply {
            groups.add(id)
            userBoxRepository.save(this)
        }

        // subscribe to group's topic
        val fcmToken: String = userRepository.findByEmail(userEmail).fcmToken
        FirebaseMessaging.getInstance().subscribeToTopic(listOf(fcmToken), id.toString())

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                id.toString()
            )
    }

    fun removeGroup(userEmail: String, groupId: String) {
        val groupObjectId = ObjectId(groupId)

        // check group is valid
        lateinit var groupEntity: GroupEntity
        runCatching {
            groupRepository.findById(groupObjectId).get()
        }.onSuccess {
            groupEntity = it
        }.onFailure {
            throw NotFoundException("Invalid GroupId: [${groupId}]")
        }

        // check user is the group owner
        if (userRepository.findByEmail(userEmail).did != groupEntity.leader) {
            throw ForbiddenException("User [$userEmail] is not a group[$groupId] owner.")
        }

        // delete images
        if (groupEntity.profile != null) photoService.deletePhoto(groupEntity.profile!!)
        if (groupEntity.header != null) photoService.deletePhoto(groupEntity.header!!)

        // delete group entity
        groupRepository.delete(groupEntity)

        // delete from all member's box
        userBoxRepository.findAll().forEach { userBox ->
            if (userBox.groups.find { it == groupObjectId } != null) {
                userBox.groups.remove(groupObjectId)
                userBoxRepository.save(userBox)
            }
        }
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

    fun joinGroup(userEmail: String, groupId: String) {
        val groupObjectId = ObjectId(groupId)

        // Find user
        lateinit var userBox: UserBox
        runCatching {
            userBoxRepository.findByEmail(userEmail)
        }.onSuccess {
            userBox = it
        }.onFailure {
            throw NotFoundException("User [${userEmail}] was not registered.")
        }

        // Check groupId is valid
        if (groupRepository.findById(groupObjectId).isEmpty)
            throw NotFoundException("Invalid GroupId: [${groupId}]")

        userBox.groups.add(groupObjectId)
        userBoxRepository.save(userBox)
    }

    fun leaveGroup(userEmail: String, groupId: String) {
        val groupObjectId = ObjectId(groupId)
        val userBox: UserBox =  userBoxRepository.findByEmail(userEmail)

        // Check groupId is valid
        if (groupRepository.findById(groupObjectId).isEmpty)
            throw NotFoundException("Invalid GroupId: [${groupId}]")

        if (userBox.groups.find { it == groupObjectId } == null) {
            throw NotFoundException("User [$userEmail] is not a group[$groupObjectId] member.")
        }

        userBox.groups.remove(groupObjectId)
        userBoxRepository.save(userBox)
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

    fun testNotification(userEmail: String) {
        val fcmToken: String = userRepository.findByEmail(userEmail).fcmToken
        val message = NotificationMessage(target = fcmToken, id = "groupId", title = "group", type = 0)
        fcmService.sendNotification(message, false)
    }
}
