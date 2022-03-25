package com.duckbox.service

import com.duckbox.domain.vote.VoteEntity
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.dto.vote.VoteRegisterDto
import com.duckbox.errors.exception.ConflictException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class VoteService (
    private val voteRepository: VoteRepository,
    private val photoService: PhotoService
) {

    fun registerVote(voteRegisterDto: VoteRegisterDto): ObjectId {
        // check duplicate
        // is Vote Title unique?
        runCatching {
            voteRepository.findByTitle(voteRegisterDto.title)
        }.onSuccess {
            throw ConflictException("Group [${voteRegisterDto.title}] is already registered.")
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
                groupId = if (voteRegisterDto.groupId == null) null else ObjectId(voteRegisterDto.groupId),
                startTime = voteRegisterDto.startTime,
                finishTime = voteRegisterDto.finishTime,
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

}
