package ro.andreidev.sms.gateway.sms.ws

import com.fasterxml.jackson.databind.ObjectMapper
import ro.andreidev.sms.gateway.sms.service.SmsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.Instant
import java.util.UUID

/**
 * Server side of the device WebSocket.
 *
 * Auth happens at handshake (see DeviceWebSocketHandshakeInterceptor): we
 * verify the Bearer token and stash the deviceId in session attributes.
 * Here we register the session, dispatch frames, and on disconnect clean up.
 */
@Component
class DeviceWebSocketHandler(
    private val registry: DeviceConnectionRegistry,
    private val pendingAcks: PendingAckRegistry,
    private val smsService: SmsService,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(DeviceWebSocketHandler::class.java)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val deviceId = session.attributes[ATTR_DEVICE_ID] as? String
        log.info("WS connected: deviceId={}", deviceId)
        if (deviceId != null) registry.register(deviceId, session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val raw = message.payload
        val deviceId = session.attributes[ATTR_DEVICE_ID] as? String ?: "?"
        val frame = runCatching { objectMapper.readValue(raw, WsFrame::class.java) }
            .onFailure { log.warn("bad frame deviceId={} payload={}", deviceId, raw, it) }
            .getOrNull() ?: run {
                sendError(session, null, WsErrorCodes.INVALID_FRAME, "Could not parse frame")
                return
            }

        if (frame !is WsFrame.Ping && frame !is WsFrame.Pong) {
            log.info(
                "recv deviceId={} type={} id={} corr={}",
                deviceId, frame::class.simpleName, frame.id, frame.correlationId
            )
        }

        when (frame) {
            is WsFrame.DeviceRegister -> handleRegister(session, frame)
            is WsFrame.Ack -> {
                log.info(
                    "ack deviceId={} providerMessageId={} state={} correlates={}",
                    deviceId, frame.providerMessageId, frame.state, frame.correlationId
                )
                pendingAcks.complete(frame)
            }
            is WsFrame.SmsStatus -> handleStatus(frame)
            is WsFrame.SmsReceived -> handleIncoming(frame)
            is WsFrame.Ping -> send(session, WsFrame.Pong(id = UUID.randomUUID().toString(), correlationId = frame.id))
            is WsFrame.Pong -> Unit
            is WsFrame.Error -> log.warn("client error deviceId={} code={} message={}", deviceId, frame.code, frame.message)
            else -> log.debug("unhandled frame deviceId={} type={}", deviceId, frame::class.simpleName)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val deviceId = session.attributes[ATTR_DEVICE_ID] as? String
        log.info("WS closed: deviceId={} status={}", deviceId, status)
        registry.unregister(session)
    }

    private fun handleRegister(session: WebSocketSession, frame: WsFrame.DeviceRegister) {
        val authDeviceId = session.attributes[ATTR_DEVICE_ID] as? String
        val accepted = authDeviceId == null || authDeviceId == frame.deviceId
        if (accepted) {
            registry.register(frame.deviceId, session)
            session.attributes[ATTR_DEVICE_ID] = frame.deviceId
        }
        send(
            session,
            WsFrame.DeviceRegisterAck(
                id = UUID.randomUUID().toString(),
                correlationId = frame.id,
                accepted = accepted,
                reason = if (accepted) null else "deviceId mismatch with handshake",
                serverTime = Instant.now().toString()
            )
        )
    }

    private fun handleStatus(frame: WsFrame.SmsStatus) {
        val updated = smsService.updateMessageStatus(frame.providerMessageId, frame.event)
        if (!updated) {
            log.warn(
                "Received status for unknown providerMessageId={} event={}",
                frame.providerMessageId, frame.event
            )
        }
    }

    private fun handleIncoming(frame: WsFrame.SmsReceived) {
        // TODO: persist inbound SMS. The current data model doesn't have an inbound
        // table; when we add it, store here and optionally fan out to a user-facing
        // webhook. For now we just log.
        log.info(
            "Incoming SMS on {} from {}: {} chars",
            frame.deviceId, frame.fromNumber, frame.content.length
        )
    }

    private fun send(session: WebSocketSession, frame: WsFrame) {
        if (!session.isOpen) return
        val text = objectMapper.writeValueAsString(frame)
        synchronized(session) { session.sendMessage(TextMessage(text)) }
    }

    private fun sendError(session: WebSocketSession, correlationId: String?, code: String, message: String) {
        send(session, WsFrame.Error(UUID.randomUUID().toString(), correlationId, code, message))
    }

    companion object {
        const val ATTR_DEVICE_ID = "deviceId"
    }
}
