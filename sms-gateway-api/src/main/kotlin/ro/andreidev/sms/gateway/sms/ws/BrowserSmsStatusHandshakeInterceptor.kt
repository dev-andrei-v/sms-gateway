package ro.andreidev.sms.gateway.sms.ws

import ro.andreidev.sms.gateway.config.JwtConfig
import ro.andreidev.sms.gateway.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class BrowserSmsStatusHandshakeInterceptor(
    private val jwtDecoder: JwtDecoder,
    private val jwtConfig: JwtConfig,
    private val userService: UserService
) : HandshakeInterceptor {
    private val log = LoggerFactory.getLogger(BrowserSmsStatusHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest
        val accessToken = servletRequest?.getParameter("access_token")?.trim().orEmpty()
        if (accessToken.isBlank()) {
            log.warn("Browser WS handshake rejected: missing access_token query parameter")
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }

        val jwt = runCatching { jwtDecoder.decode(accessToken) }
            .onFailure { log.warn("Browser WS handshake rejected: invalid JWT", it) }
            .getOrNull() ?: run {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }

        val authorities = jwtConfig.authConverter()
            .convert(jwt)
            ?.authorities
            ?.map { it.authority }
            .orEmpty()

        if (authorities.none { it == "ROLE_sms_gateway_user" || it == "ROLE_sms_gateway_admin" }) {
            log.warn("Browser WS handshake rejected: missing SMS roles for subject={}", jwt.subject)
            response.setStatusCode(HttpStatus.FORBIDDEN)
            return false
        }

        val user = userService.resolveFromJwt(jwt)
        if (!user.enabled) {
            log.warn("Browser WS handshake rejected: disabled user {}", user.username)
            response.setStatusCode(HttpStatus.FORBIDDEN)
            return false
        }

        val userId = user.id
        if (userId == null) {
            log.warn("Browser WS handshake rejected: unresolved user id for {}", user.username)
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }

        attributes[BrowserSmsStatusWebSocketHandler.ATTR_USER_ID] = userId
        attributes[BrowserSmsStatusWebSocketHandler.ATTR_USERNAME] = user.username
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) = Unit
}
