package ro.andreidev.sms.gateway.sms.ws

import com.fasterxml.jackson.databind.ObjectMapper
import ro.andreidev.sms.gateway.sms.dto.MessageResponse
import ro.andreidev.sms.gateway.sms.entity.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage

@Component
class SmsStatusNotifier(
    private val registry: BrowserSmsSubscriptionRegistry,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(SmsStatusNotifier::class.java)

    fun publish(message: Message) {
        val messageId = message.id ?: return
        val sessions = registry.sessions(messageId)
        if (sessions.isEmpty()) return

        val frame = SmsStatusFrame(
            type = "message_status",
            message = MessageResponse.from(message)
        )
        val payload = objectMapper.writeValueAsString(frame)

        sessions.forEach { session ->
            try {
                synchronized(session) {
                    if (session.isOpen) {
                        session.sendMessage(TextMessage(payload))
                    }
                }
            } catch (t: Throwable) {
                log.warn("Failed to publish SMS status to browser session={}", session.id, t)
                registry.unregister(session)
            }
        }
    }
}

data class SmsStatusFrame(
    val type: String,
    val message: MessageResponse
)
