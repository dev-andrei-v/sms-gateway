package ro.andreidev.sms.gateway.sms.ws

import com.fasterxml.jackson.databind.ObjectMapper
import ro.andreidev.sms.gateway.sms.dto.MessageResponse
import ro.andreidev.sms.gateway.sms.repository.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class BrowserSmsStatusWebSocketHandler(
    private val registry: BrowserSmsSubscriptionRegistry,
    private val messageRepository: MessageRepository,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(BrowserSmsStatusWebSocketHandler::class.java)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.attributes[ATTR_USER_ID] as? Long
        val username = session.attributes[ATTR_USERNAME] as? String ?: "?"
        if (userId == null) {
            log.warn("Browser WS connected without resolved user id, closing session={}", session.id)
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }

        registry.register(userId, session)
        log.info("Browser WS connected: userId={} username={} session={}", userId, username, session.id)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        registry.unregister(session)
        log.info("Browser WS closed: session={} status={}", session.id, status)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val frame = runCatching { objectMapper.readValue(message.payload, BrowserSmsClientFrame::class.java) }
            .onFailure { log.warn("Browser WS bad frame: session={} payload={}", session.id, message.payload, it) }
            .getOrNull() ?: run {
            send(session, BrowserSmsServerFrame(type = "error", message = "Invalid browser WS frame"))
            return
        }

        val userId = registry.userId(session)
        if (userId == null) {
            send(session, BrowserSmsServerFrame(type = "error", message = "Unauthenticated session"))
            return
        }

        val messageId = frame.messageId
        if (messageId == null) {
            send(session, BrowserSmsServerFrame(type = "error", message = "messageId is required"))
            return
        }

        val ownedMessage = messageRepository.findByIdAndUser_Id(messageId, userId)

        if (ownedMessage == null) {
            send(session, BrowserSmsServerFrame(type = "error", message = "Message not found"))
            return
        }

        when (frame.action.lowercase()) {
            "subscribe" -> {
                registry.subscribe(session, messageId)
                send(session, BrowserSmsServerFrame(type = "subscribed", messageId = messageId))
                send(session, SmsStatusFrame(type = "message_status", message = MessageResponse.from(ownedMessage)))
            }
            "unsubscribe" -> {
                registry.unsubscribe(session, messageId)
                send(session, BrowserSmsServerFrame(type = "unsubscribed", messageId = messageId))
            }
            else -> send(session, BrowserSmsServerFrame(type = "error", message = "Unsupported action"))
        }
    }

    private fun send(session: WebSocketSession, frame: Any) {
        if (!session.isOpen) return
        val payload = objectMapper.writeValueAsString(frame)
        synchronized(session) { session.sendMessage(TextMessage(payload)) }
    }

    companion object {
        const val ATTR_USER_ID = "browserUserId"
        const val ATTR_USERNAME = "browserUsername"
    }
}

data class BrowserSmsClientFrame(
    val action: String = "",
    val messageId: Long? = null
)

data class BrowserSmsServerFrame(
    val type: String,
    val messageId: Long? = null,
    val message: String? = null
)
