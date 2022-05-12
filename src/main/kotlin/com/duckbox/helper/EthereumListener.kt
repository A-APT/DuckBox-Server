package com.duckbox.helper

import com.duckbox.domain.group.GroupEntity
import com.duckbox.domain.group.GroupRepository
import com.duckbox.domain.group.GroupStatus
import com.duckbox.domain.user.UserRepository
import com.duckbox.dto.notification.NotificationMessage
import com.duckbox.service.FCMService
import com.google.firebase.messaging.FirebaseMessaging
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Component
import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import javax.annotation.PostConstruct
import javax.xml.bind.DatatypeConverter

@PropertySource("classpath:ethereum.properties")
@Component
class EthereumListener(
    private val web3j: Web3j,
    private val groupRepository: GroupRepository,
    private val fcmService: FCMService,
    private val userRepository: UserRepository,
) {

    @Value("\${contract.address.groups}")
    private lateinit var groupsAddress: String

    @PostConstruct
    fun listenGroupAuthCompleted() {
        val ethFilter: EthFilter = EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST, groupsAddress)
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

    @PostConstruct
    fun listenMemberAuthCompleted() {
        val ethFilter: EthFilter = EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST, groupsAddress)
        val event: Event = Event("memberAuthCompleted", listOf(object: TypeReference<Utf8String>() {}, object: TypeReference<Bytes32>() {}))
        val encodedEventSignature: String = EventEncoder.encode(event)
        ethFilter.addSingleTopic(encodedEventSignature)
        web3j.ethLogFlowable(ethFilter).subscribe { // keep listening to event
            // group member's mutual authentication is completed
            val decodedList = FunctionReturnDecoder.decode(it.data, event.parameters)
            val groupId: String = decodedList[0].value as String
            val did: String = DatatypeConverter.printHexBinary(decodedList[1].value as ByteArray)

            // send notification to user
            val fcmToken: String = userRepository.findByDid(did).fcmToken
            fcmService.sendNotification(
                notification = NotificationMessage(target = fcmToken, title = "group", message = groupId),
                isTopic = false
            )

            // subscribe to group's topic
            FirebaseMessaging.getInstance().subscribeToTopic(listOf(fcmToken), groupId)
        }
    }
}
