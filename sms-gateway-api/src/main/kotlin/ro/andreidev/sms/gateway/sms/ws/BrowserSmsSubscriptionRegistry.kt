package ro.andreidev.sms.gateway.sms.ws

import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class BrowserSmsSubscriptionRegistry {
    private val userIdBySessionId = ConcurrentHashMap<String, Long>()
    private val sessionById = ConcurrentHashMap<String, WebSocketSession>()
    private val messageIdsBySessionId = ConcurrentHashMap<String, MutableSet<Long>>()
    private val sessionIdsByMessageId = ConcurrentHashMap<Long, MutableSet<String>>()

    fun register(userId: Long, session: WebSocketSession) {
        userIdBySessionId[session.id] = userId
        sessionById[session.id] = session
    }

    fun unregister(session: WebSocketSession) {
        val sessionId = session.id
        userIdBySessionId.remove(sessionId)
        sessionById.remove(sessionId)
        messageIdsBySessionId.remove(sessionId)?.forEach { messageId ->
            sessionIdsByMessageId.computeIfPresent(messageId) { _, existing ->
                existing.remove(sessionId)
                existing.takeIf { it.isNotEmpty() }
            }
        }
    }

    fun userId(session: WebSocketSession): Long? = userIdBySessionId[session.id]

    fun subscribe(session: WebSocketSession, messageId: Long) {
        messageIdsBySessionId.compute(session.id) { _, existing ->
            (existing ?: ConcurrentHashMap.newKeySet()).apply { add(messageId) }
        }
        sessionIdsByMessageId.compute(messageId) { _, existing ->
            (existing ?: ConcurrentHashMap.newKeySet()).apply { add(session.id) }
        }
    }

    fun unsubscribe(session: WebSocketSession, messageId: Long) {
        messageIdsBySessionId.computeIfPresent(session.id) { _, existing ->
            existing.remove(messageId)
            existing.takeIf { it.isNotEmpty() }
        }
        sessionIdsByMessageId.computeIfPresent(messageId) { _, existing ->
            existing.remove(session.id)
            existing.takeIf { it.isNotEmpty() }
        }
    }

    fun sessions(messageId: Long): List<WebSocketSession> =
        sessionIdsByMessageId[messageId]
            ?.mapNotNull(sessionById::get)
            ?.filter { it.isOpen }
            .orEmpty()
}
