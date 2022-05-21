package com.duckbox.helper

import com.duckbox.domain.vote.BallotStatus
import com.duckbox.domain.vote.VoteEntity
import com.duckbox.domain.vote.VoteRepository
import com.duckbox.service.ethereum.BallotService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

@Component
class VoteScheduler (
    private val voteRepository: VoteRepository,
    private val ballotService: BallotService,
){

    @Scheduled(cron = "0 5 * * * ?")
    fun voteStatusTask() {
        // every 5 minute, check vote startTime and finishTime
        val now: Date = Date()

        // try to close vote for OPEN vote
        val openVoteList: MutableList<VoteEntity> = voteRepository.findAllByStatus(BallotStatus.OPEN)
        openVoteList.forEach {
            if (it.finishTime <= now) {
                // to ethereum
                ballotService.close(it.id.toString(), it.voteNum)

                it.status = BallotStatus.FINISHED
                voteRepository.save(it)
            }
        }

        // try to start vote for REGISTERED vote
        val registeredVoteList: MutableList<VoteEntity> = voteRepository.findAllByStatus(BallotStatus.REGISTERED)
        registeredVoteList.forEach {
            if (it.startTime <= now) {
                // to ethereum
                ballotService.open(it.id.toString())

                it.status = BallotStatus.OPEN
                voteRepository.save(it)
            }
        }
    }

}
