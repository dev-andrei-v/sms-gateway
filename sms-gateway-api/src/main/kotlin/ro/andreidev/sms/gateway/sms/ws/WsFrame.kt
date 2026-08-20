package ro.andreidev.sms.gateway.sms.ws

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Wire-compatible mirror of the Android client's WsProtocol.
 * Keep these classes in lockstep with `ro.andreidev.sms.middleware.ws.WsProtocol`
 * in sms-gateway-android-client. Same JSON, same semantics.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = WsFrame.DeviceRegister::class, name = "device_register"),
    JsonSubTypes.Type(value = WsFrame.DeviceRegisterAck::class, name = "device_register_ack"),
    JsonSubTypes.Type(value = WsFrame.SendSms::class, name = "send_sms"),
    JsonSubTypes.Type(value = WsFrame.Ack::class, name = "ack"),
    JsonSubTypes.Type(value = WsFrame.SmsStatus::class, name = "sms_status"),
    JsonSubTypes.Type(value = WsFrame.SmsReceived::class, name = "sms_received"),
    JsonSubTypes.Type(value = WsFrame.Ping::class, name = "ping"),
    JsonSubTypes.Type(value = WsFrame.Pong::class, name = "pong"),
    JsonSubTypes.Type(value = WsFrame.Error::class, name = "error"),
)
sealed class WsFrame {
    abstract val id: String
    abstract val correlationId: String?

    data class DeviceRegister(
        override val id: String,
        override val correlationId: String? = null,
        val deviceId: String,
        val deviceName: String,
        val osVersion: String,
        val appVersion: String,
        val protocolVersion: Int = 1,
        val capabilities: List<String> = emptyList()
    ) : WsFrame()

    data class DeviceRegisterAck(
        override val id: String,
        override val correlationId: String?,
        val accepted: Boolean,
        val reason: String? = null,
        val serverTime: String? = null
    ) : WsFrame()

    data class SendSms(
        override val id: String,
        override val correlationId: String? = null,
        val providerMessageId: String,
        val phoneNumber: String,
        val content: String,
        val simSlot: Int? = null
    ) : WsFrame()

    data class Ack(
        override val id: String,
        override val correlationId: String?,
        val deviceId: String,
        val providerMessageId: String,
        val state: String = "PENDING"
    ) : WsFrame()

    data class SmsStatus(
        override val id: String,
        override val correlationId: String? = null,
        val deviceId: String,
        val providerMessageId: String,
        val event: String,
        val reason: String? = null,
        val occurredAt: String
    ) : WsFrame()

    data class SmsReceived(
        override val id: String,
        override val correlationId: String? = null,
        val deviceId: String,
        val fromNumber: String,
        val content: String,
        val receivedAt: String,
        val simSlot: Int? = null
    ) : WsFrame()

    data class Ping(
        override val id: String,
        override val correlationId: String? = null
    ) : WsFrame()

    data class Pong(
        override val id: String,
        override val correlationId: String?
    ) : WsFrame()

    data class Error(
        override val id: String,
        override val correlationId: String?,
        val code: String,
        val message: String
    ) : WsFrame()
}

object SmsEvents {
    const val SENT = "sms:sent"
    const val DELIVERED = "sms:delivered"
    const val FAILED = "sms:failed"
}

object WsErrorCodes {
    const val UNAUTHENTICATED = "UNAUTHENTICATED"
    const val INVALID_FRAME = "INVALID_FRAME"
    const val UNSUPPORTED_VERSION = "UNSUPPORTED_VERSION"
    const val INTERNAL = "INTERNAL"
}
