package ro.andreidev.sms.gateway.sms.ws

import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

/**
 * Validates device credentials before upgrading the HTTP request to a WebSocket.
 *
 * Expects:
 *  - Authorization: Bearer <api-key>
 *  - X-Device-Id: <uuid>   (advisory, validated against the register frame later)
 *
 * Rejects with 401 if the API key is missing or invalid. On success, stashes
 * `deviceId` in the session attributes so the handler can pick it up.
 */
@Component
class DeviceWebSocketHandshakeInterceptor(
    private val verifier: DeviceApiKeyVerifier
) : HandshakeInterceptor {

    private val log = LoggerFactory.getLogger(DeviceWebSocketHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val authHeader = request.headers.getFirst("Authorization")
        val apiKey = authHeader
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substring("Bearer ".length)
            ?.trim()

        if (apiKey.isNullOrBlank()) {
            log.warn("WS handshake rejected: missing Bearer token")
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }
        val deviceId = request.headers.getFirst("X-Device-Id")
        if (!verifier.verify(apiKey, deviceId)) {
            log.warn("WS handshake rejected: bad API key (deviceId={})", deviceId)
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }
        if (deviceId != null) attributes[DeviceWebSocketHandler.ATTR_DEVICE_ID] = deviceId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        // no-op
    }
}
