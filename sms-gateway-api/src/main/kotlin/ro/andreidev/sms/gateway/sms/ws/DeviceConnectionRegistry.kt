package ro.andreidev.sms.gateway.sms.ws

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which devices are currently connected and routes outbound commands
 * to the right socket.
 *
 * Authoritative on a single instance only. If you horizontally scale the
 * backend, replace this with a redis-backed registry plus a pub/sub channel
 * for cross-node routing. For a home-lab deployment a single instance is fine.
 */
@Component
class DeviceConnectionRegistry {
    private val log = LoggerFactory.getLogger(DeviceConnectionRegistry::class.java)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    fun register(deviceId: String, session: WebSocketSession) {
        val prev = sessions.put(deviceId, session)
        if (prev != null && prev !== session && prev.isOpen) {
            log.info("Replacing existing session for device {}", deviceId)
            runCatching { prev.close() }
        }
    }

    fun unregister(session: WebSocketSession) {
        // Remove by value — handler may not know its own deviceId if registration
        // never completed.
        val toDrop = sessions.entries.firstOrNull { it.value === session }?.key
        if (toDrop != null) sessions.remove(toDrop, session)
    }

    fun session(deviceId: String): WebSocketSession? = sessions[deviceId]?.takeIf { it.isOpen }

    fun connectedDevices(): Set<String> = sessions.keys.toSet()

    fun anyConnected(): String? = sessions.entries.firstOrNull { it.value.isOpen }?.key
}
