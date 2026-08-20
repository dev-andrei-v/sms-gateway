package ro.andreidev.sms.gateway.sms.client

import com.fasterxml.jackson.databind.ObjectMapper
import ro.andreidev.sms.gateway.sms.ws.DeviceConnectionRegistry
import ro.andreidev.sms.gateway.sms.ws.PendingAckRegistry
import ro.andreidev.sms.gateway.sms.ws.WsFrame
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Sends SMS via the device WebSocket instead of HTTP-POSTing the phone directly.
 *
 * Flow:
 *   1. Pick a connected device (single-device today; extend to route-by-user).
 *   2. Build a `send_sms` frame, register a pending-ack future.
 *   3. Write the frame to the session; block until we receive the matching Ack
 *      or the configured timeout elapses.
 *
 * The returned SmsSendResponse has state=PENDING on successful ack — the actual
 * "sent / delivered" transitions arrive later as `sms_status` frames and flow
 * into SmsService.updateMessageStatus.
 */
@Component
class WebSocketSmsGatewayClient(
    private val registry: DeviceConnectionRegistry,
    private val pendingAcks: PendingAckRegistry,
    private val objectMapper: ObjectMapper,
    @Value("\${sms-gateway.device-ws.ack-timeout-seconds:30}") private val ackTimeoutSeconds: Long
) : SmsGatewayClient {

    private val log = LoggerFactory.getLogger(WebSocketSmsGatewayClient::class.java)

    override fun sendMessage(phoneNumber: String, message: String): SmsGatewayClient.SmsSendResponse {
        val deviceId = registry.anyConnected() ?: run {
            log.error("No device connected — cannot send SMS")
            return SmsGatewayClient.SmsSendResponse(state = SmsGatewayClient.SmsProcessingState.FAILED)
        }
        val session = registry.session(deviceId) ?: run {
            log.error("Session for device {} disappeared", deviceId)
            return SmsGatewayClient.SmsSendResponse(state = SmsGatewayClient.SmsProcessingState.FAILED)
        }

        val commandId = UUID.randomUUID().toString()
        val providerMessageId = "p-" + UUID.randomUUID().toString()
        val frame = WsFrame.SendSms(
            id = commandId,
            providerMessageId = providerMessageId,
            phoneNumber = phoneNumber,
            content = message
        )

        val future = pendingAcks.await(commandId)
        return try {
            val json = objectMapper.writeValueAsString(frame)
            synchronized(session) { session.sendMessage(TextMessage(json)) }
            val ack = future.get(ackTimeoutSeconds, TimeUnit.SECONDS)
            SmsGatewayClient.SmsSendResponse(
                state = mapState(ack.state),
                providerId = ack.providerMessageId
            )
        } catch (timeout: TimeoutException) {
            pendingAcks.fail(commandId, "ack_timeout")
            log.error("SMS send ack timed out for providerMessageId={}", providerMessageId)
            SmsGatewayClient.SmsSendResponse(state = SmsGatewayClient.SmsProcessingState.UNKNOWN)
        } catch (t: Throwable) {
            pendingAcks.fail(commandId, t.message ?: "error")
            log.error("SMS send failed", t)
            SmsGatewayClient.SmsSendResponse(state = SmsGatewayClient.SmsProcessingState.FAILED)
        }
    }

    private fun mapState(raw: String?): SmsGatewayClient.SmsProcessingState =
        when (raw?.trim()?.uppercase()) {
            "PENDING" -> SmsGatewayClient.SmsProcessingState.PENDING
            "SENT" -> SmsGatewayClient.SmsProcessingState.SENT
            "DELIVERED" -> SmsGatewayClient.SmsProcessingState.DELIVERED
            "FAILED" -> SmsGatewayClient.SmsProcessingState.FAILED
            else -> SmsGatewayClient.SmsProcessingState.UNKNOWN
        }
}
