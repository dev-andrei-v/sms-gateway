package ro.andreidev.sms.gateway.sms.dto

import ro.andreidev.sms.gateway.sms.entity.Message
import ro.andreidev.sms.gateway.sms.entity.MessageStatus
import java.time.Instant

data class MessageResponse(
    val id: Long,
    val phoneNumber: String,
    val content: String,
    val status: MessageStatus,
    val createdAt: Instant,
) {
    companion object {
        fun from(message: Message) = MessageResponse(
            id = message.id!!,
            phoneNumber = message.phoneNumber,
            content = message.content,
            status = message.status,
            createdAt = message.createDate,
        )
    }
}
