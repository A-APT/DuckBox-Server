package com.duckbox.service

import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserBox
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.VoteEntity
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.JWTToken
import com.duckbox.dto.user.LoginRequestDto
import com.duckbox.dto.user.LoginResponseDto
import com.duckbox.dto.user.RegisterDto
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.NotFoundException
import com.duckbox.security.JWTTokenProvider
import com.duckbox.service.ethereum.DIdService
import com.duckbox.utils.HashUtils
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class UserService (
    private val userRepository: UserRepository,
    private val userBoxRepository: UserBoxRepository,
    private val groupRepository: GroupRepository,
    private val voteRepository: VoteRepository,
    private val jwtTokenProvider: JWTTokenProvider,
    private val hashUtils: HashUtils,
    private val didService: DIdService
    ) {

    fun generateUserDID(targetEmail: String, targetNumber: String): String {
        // generate did using SHA-256: current time + email + phone-number
        val didInfo = "${System.currentTimeMillis()}${targetEmail}"
        return hashUtils.SHA256(didInfo)
    }

    fun register(registerDto: RegisterDto) {
        // check duplicate
        runCatching {
            userRepository.findByEmail(registerDto.email)
        }.onSuccess {
            throw ConflictException("User email [${registerDto.email}] is already registered.")
        }

        // send transaction
        val did = generateUserDID(registerDto.email, registerDto.phoneNumber)
        //didService.registerDid(did)

        // save to server
        val id = userRepository.save(
            User(
                did = did,
                studentId = registerDto.studentId,
                name = registerDto.name,
                password = registerDto.password,
                email = registerDto.email,
                phoneNumber = registerDto.phoneNumber,
                nickname = registerDto.nickname,
                college = registerDto.college,
                department = registerDto.department,
                roles = setOf("ROLE_USER")
            )
        ).id
        userBoxRepository.save(
            UserBox(
                id = id,
                email = registerDto.email,
                groups = mutableListOf(),
                votes = mutableListOf(),
            )
        )
    }

    fun login(loginRequestDto: LoginRequestDto): ResponseEntity<LoginResponseDto> {
        // Find user
        lateinit var user: User
        runCatching {
            userRepository.findByEmail(loginRequestDto.email)
        }.onSuccess {
            user = it
        }.onFailure {
            throw NotFoundException("User [${loginRequestDto.email}] was not registered.")
        }

        // Check password
        if (user.password != loginRequestDto.password) {
            throw NotFoundException("User email or password was wrong.")
        }

        // Generate JWT token
        val jwtToken: JWTToken = jwtTokenProvider.generateToken(user.email, user.roles.toList())
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                LoginResponseDto(token = jwtToken.token, refreshToken = jwtToken.refreshToken)
            )
    }

    fun refreshToken(refreshToken: String): ResponseEntity<JWTToken> { // refresh JWT token
        val jwtToken: JWTToken = jwtTokenProvider.refreshToken(refreshToken)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                JWTToken(token = jwtToken.token, refreshToken = jwtToken.refreshToken)
            )
    }

    fun joinGroup(userEmail: String, groupId: ObjectId) {
        // Find user
        lateinit var userBox: UserBox
        runCatching {
            userBoxRepository.findByEmail(userEmail)
        }.onSuccess {
            userBox = it
        }.onFailure {
            throw NotFoundException("User [${userEmail}] was not registered.")
        }

        // Check voteId is valid
        if (groupRepository.findById(groupId).isEmpty)
            throw NotFoundException("Invalid GroupId: [${groupId}]")

        userBox.groups.add(groupId)
        userBoxRepository.save(userBox)
    }

    fun joinVote(userEmail: String, voteId: ObjectId) {
        // Find user
        lateinit var userBox: UserBox
        runCatching {
            userBoxRepository.findByEmail(userEmail)
        }.onSuccess {
            userBox = it
        }.onFailure {
            throw NotFoundException("User [${userEmail}] was not registered.")
        }

        // Check voteId is valid
        if (voteRepository.findById(voteId).isEmpty)
            throw NotFoundException("Invalid VoteId: [${voteId}]")

        userBox.votes.add(voteId)
        userBoxRepository.save(userBox)
    }

}
