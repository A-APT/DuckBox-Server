package com.duckbox.service

import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.user.UserRepository
import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteEntity
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.errors.exception.NotFoundException
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class VoteService (
    private val voteRepository: VoteRepository,
    private val photoService: PhotoService,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
) {

    fun registerVote(userEmail: String, voteRegisterDto: VoteRegisterDto): ObjectId {
        lateinit var ownerId: String
        if (voteRegisterDto.isGroup) { // check groupId is valid
            runCatching {
                groupRepository.findById(ObjectId(voteRegisterDto.owner)).get()
            }.onFailure {
                throw NotFoundException("Invalid GroupId: [${voteRegisterDto.owner}]")
            }
            ownerId = voteRegisterDto.owner!!
        } else {
            ownerId = userRepository.findByEmail(userEmail).id.toString()
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
                owner = ownerId,
                startTime = voteRegisterDto.startTime,
                finishTime = voteRegisterDto.finishTime,
                status = BallotStatus.OPEN,
                images = idOfImages.toList(),
                candidates = voteRegisterDto.candidates,
                voters = voteRegisterDto.voters,
                reward = voteRegisterDto.reward
            )
        ).id

        if (voteRegisterDto.notice) {
            // TODO
            // notice to voters
        }

        return id
    }

    fun findVotesOfGroup(_groupId: String): ResponseEntity<List<VoteDetailDto>> {
        val groupId: ObjectId = ObjectId(_groupId) // invalid group returns 0 size voteList
        val voteList: MutableList<VoteDetailDto> = mutableListOf()
        voteRepository.findAllByOwner(groupId.toString()).forEach {
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
}
