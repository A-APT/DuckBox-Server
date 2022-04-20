package com.duckbox.service

import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.user.User
import com.duckbox.domain.user.UserBox
import com.duckbox.domain.user.UserBoxRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteEntity
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.user.BlingSigRequestDto
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.dto.vote.VoteToken
import com.duckbox.errors.exception.ConflictException
import com.duckbox.errors.exception.ForbiddenException
import com.duckbox.errors.exception.NotFoundException
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigInteger

@Service
class VoteService (
    private val voteRepository: VoteRepository,
    private val photoService: PhotoService,
    private val userRepository: UserRepository,
    private val userBoxRepository: UserBoxRepository,
    private val groupRepository: GroupRepository,
    private val blindSignatureService: BlindSignatureService,
) {

    fun registerVote(userEmail: String, voteRegisterDto: VoteRegisterDto): ResponseEntity<String> {
        val owner: String = userRepository.findByEmail(userEmail).nickname
        if (voteRegisterDto.isGroup) { // check groupId is valid
            runCatching {
                groupRepository.findById(ObjectId(voteRegisterDto.groupId)).get()
            }.onFailure {
                throw NotFoundException("Invalid GroupId: [${voteRegisterDto.groupId}]")
            }
        }

        // upload images if exists
        val idOfImages: MutableList<ObjectId> = mutableListOf()
        voteRegisterDto.images.forEach {
            idOfImages.add(photoService.savePhoto(it))
        }

        // save to server
        val id = voteRepository.save(
            VoteEntity(
                title = voteRegisterDto.title,
                content = voteRegisterDto.content,
                isGroup = voteRegisterDto.isGroup,
                groupId = voteRegisterDto.groupId,
                owner = owner,
                ownerPrivate = BigInteger(voteRegisterDto.ownerPrivate, 16),
                startTime = voteRegisterDto.startTime,
                finishTime = voteRegisterDto.finishTime,
                status = BallotStatus.REGISTERED,
                images = idOfImages.toList(),
                candidates = voteRegisterDto.candidates,
                voters = voteRegisterDto.voters,
                voteNum = 0,
                reward = voteRegisterDto.reward
            )
        ).id

        if (voteRegisterDto.notice) {
            // TODO
            // notice to voters
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                id.toString()
            )
    }

    fun getAllVote(): ResponseEntity<List<VoteDetailDto>> {
        val voteList: MutableList<VoteDetailDto> = mutableListOf()
        voteRepository.findAll().forEach {
            // get images
            val images: MutableList<ByteArray> = mutableListOf()
            it.images.forEach { photoId ->
                images.add(photoService.getPhoto(photoId).data)
            }
            voteList.add(it.toVoteDetailDto(images))
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                voteList
            )
    }

    fun findVotesOfGroup(_groupId: String): ResponseEntity<List<VoteDetailDto>> {
        val groupId: ObjectId = ObjectId(_groupId) // invalid group returns 0 size voteList
        val voteList: MutableList<VoteDetailDto> = mutableListOf()
        voteRepository.findAllByGroupId(groupId.toString()).forEach {
            // get images
            val images: MutableList<ByteArray> = mutableListOf()
            it.images.forEach { photoId ->
                images.add(photoService.getPhoto(photoId).data)
            }
            voteList.add(it.toVoteDetailDto(images))
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                voteList
            )
    }

    fun generateBlindSigVoteToken(userEmail: String, blindSigRequestDto: BlingSigRequestDto): ResponseEntity<VoteToken> {
        val voteObjectId = ObjectId(blindSigRequestDto.targetId)

        // Find user
        lateinit var user: User
        runCatching {
            userRepository.findByEmail(userEmail)
        }.onSuccess {
            user = it
        }.onFailure {
            throw NotFoundException("User [${userEmail}] was not registered.")
        }
        val userBox: UserBox = userBoxRepository.findByEmail(userEmail)

        // Find vote entity
        lateinit var vote: VoteEntity
        runCatching {
            voteRepository.findById(voteObjectId).get()
        }.onSuccess {
            vote = it
        }.onFailure {
            throw NotFoundException("Invalid VoteId: [${blindSigRequestDto.targetId}]")
        }

        // check whether user already received vote token (signature)
        if (userBox.votes.find { it == voteObjectId } != null) {
            throw ConflictException("User [$userEmail] has already participated in the vote [${blindSigRequestDto.targetId}].")
        }

        // check user is eligible
        if (vote.isGroup) {
            if (vote.voters == null) {
                // all group member have right to vote
                if (userBox.groups.find { it == ObjectId(vote.groupId) } == null) {
                    // but ineligible when user is not a group member
                    throw ForbiddenException("User [$userEmail] is ineligible for vote [${blindSigRequestDto.targetId}].")
                }
            } else if (vote.voters?.find { it == user.studentId } == null) {
                // only vote.voters have right to vote
                throw ForbiddenException("User [$userEmail] is ineligible for vote [${blindSigRequestDto.targetId}].")
            }
        } // else, all user have right to vote (== community)

        // generate 2 blind signatures: sign with Server's key and VoteOwner's key
        val blindMessage: BigInteger = BigInteger(blindSigRequestDto.blindMessage, 16)
        val blindSigOfServer: BigInteger = blindSignatureService.blindSig(blindMessage)
        val blindSigOfVoteOwner: BigInteger = blindSignatureService.blindSig(vote.ownerPrivate, blindMessage)

        // update user's voting record
        userBox.votes.add(voteObjectId)
        userBoxRepository.save(userBox)

        // update vote's voteNum record
        vote.voteNum++
        voteRepository.save(vote)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                VoteToken( // in radix 16
                    serverToken = blindSigOfServer.toString(16),
                    voteOwnerToken = blindSigOfVoteOwner.toString(16)
                )
            )
    }
}
