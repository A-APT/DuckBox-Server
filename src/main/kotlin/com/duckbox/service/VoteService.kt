package com.duckbox.service

import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteEntity
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.vote.VoteDetailDto
import com.duckbox.dto.vote.VoteRegisterDto
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class VoteService (
    private val voteRepository: VoteRepository,
    private val photoService: PhotoService,
) {

    fun registerVote(voteRegisterDto: VoteRegisterDto): ObjectId {

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
                groupId = if (voteRegisterDto.groupId == null) null else ObjectId(voteRegisterDto.groupId),
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
        voteRepository.findAllByGroupId(groupId).forEach {
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
