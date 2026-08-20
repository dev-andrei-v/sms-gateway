package ro.andreidev.sms.middleware.ws

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Wire format between the Android middleware client and the backend.
 *
 * Every frame is a JSON object with a `type` field selecting the concrete class.
 * Frames carry an `id` for idempotency and optionally a `correlationId` tying a
 * response to a prior request.
 *
 * Protocol version is exchanged in DeviceRegister and defaults to 1.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class WsFrame {
    abstract val id: String
    abstract val correlationId: String?

    /** Device → Backend: first frame after connect; identifies the device and its capabilities. */
    @Serializable
    @kotlinx.serialization.SerialName("device_register")
    data class DeviceRegister(
        override val id: String,
        override val correlationId: String? = null,
        val deviceId: String,
        val deviceName: String,
        val osVersion: String,
        val appVersion: String,
        val protocolVersion: Int = 1,
        val capabilities: List<String> = listOf("sms_send", "sms_receive")
    ) : WsFrame()

    /** Backend → Device: ack of DeviceRegister. */
    @Serializable
    @kotlinx.serialization.SerialName("device_register_ack")
    data class DeviceRegisterAck(
        override val id: String,
        override val correlationId: String?,
        val accepted: Boolean,
        val reason: String? = null,
        val serverTime: String? = null
    ) : WsFrame()

    /** Backend → Device: send an SMS. Device responds with `Ack` then emits `SmsStatus` frames as the state evolves. */
    @Serializable
    @kotlinx.serialization.SerialName("send_sms")
    data class SendSms(
        override val id: String,
        override val correlationId: String? = null,
        val providerMessageId: String,
        val phoneNumber: String,
        val content: String,
        val simSlot: Int? = null
    ) : WsFrame()

    /** Device → Backend: immediate acknowledgement that a command was received and queued locally. */
    @Serializable
    @kotlinx.serialization.SerialName("ack")
    data class Ack(
        override val id: String,
        override val correlationId: String?,
        val deviceId: String,
        val providerMessageId: String,
        val state: String = "PENDING"
    ) : WsFrame()

    /** Device → Backend: SMS status event (sent / delivered / failed). */
    @Serializable
    @kotlinx.serialization.SerialName("sms_status")
    data class SmsStatus(
        override val id: String,
        override val correlationId: String? = null,
        val deviceId: String,
        val providerMessageId: String,
        val event: String,
        val reason: String? = null,
        val occurredAt: String
    ) : WsFrame()

    /** Device → Backend: a new SMS arrived on the phone. */
    @Serializable
    @kotlinx.serialization.SerialName("sms_received")
    data class SmsReceived(
        override val id: String,
        override val correlationId: String? = null,
        val deviceId: String,
        val fromNumber: String,
        val content: String,
        val receivedAt: String,
        val simSlot: Int? = null
    ) : WsFrame()

    /** Device ↔ Backend: keepalive. */
    @Serializable
    @kotlinx.serialization.SerialName("ping")
    data class Ping(
        override val id: String,
        override val correlationId: String? = null
    ) : WsFrame()

    @Serializable
    @kotlinx.serialization.SerialName("pong")
    data class Pong(
        override val id: String,
        override val correlationId: String?
    ) : WsFrame()

    /** Either side: signals a protocol or processing error. */
    @Serializable
    @kotlinx.serialization.SerialName("error")
    data class Error(
        override val id: String,
        override val correlationId: String?,
        val code: String,
        val message: String
    ) : WsFrame()
}

/** Well-known SMS status event values — must stay in sync with the backend's webhook mapping. */
object SmsEvents {
    const val SENT = "sms:sent"
    const val DELIVERED = "sms:delivered"
    const val FAILED = "sms:failed"
}

/** Known error codes the server may return. */
object WsErrorCodes {
    const val UNAUTHENTICATED = "UNAUTHENTICATED"
    const val INVALID_FRAME = "INVALID_FRAME"
    const val UNSUPPORTED_VERSION = "UNSUPPORTED_VERSION"
    const val INTERNAL = "INTERNAL"
}

object WsJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
        explicitNulls = false
    }
}
