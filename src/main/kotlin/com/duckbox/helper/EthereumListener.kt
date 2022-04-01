package com.duckbox.helper

import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.Utf8String
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import javax.annotation.PostConstruct

@Component
class EthereumListener(
    private val web3j: Web3j,
    private val groupRepository: GroupRepository,
) {

    @PostConstruct
    fun listenGroupAuthCompleted() {
        val contractAddress = "0x2e9C4AE65A2b73Ec006ceE643a3e0160bc019E57" // temp
        val ethFilter: EthFilter = EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST, contractAddress)
        // val event: Event = Event("increaseCalled", listOf(object: TypeReference<Int>() {}))
        val event: Event = Event("groupAuthCompleted", listOf(object: TypeReference<Utf8String>() {}))
        val encodedEventSignature: String = EventEncoder.encode(event)
        ethFilter.addSingleTopic(encodedEventSignature)
        web3j.ethLogFlowable(ethFilter).subscribe { // keep listening to event
            // change group status
            val groupId: String = FunctionReturnDecoder.decode(it.data, event.parameters)[0].value as String
            val groupEntity: GroupEntity = groupRepository.findById(ObjectId(groupId)).get()
            groupEntity.status = GroupStatus.VALID
            groupRepository.save(groupEntity)
        }
    }
}
