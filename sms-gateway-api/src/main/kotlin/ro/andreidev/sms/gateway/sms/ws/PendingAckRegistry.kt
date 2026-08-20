package ro.andreidev.sms.gateway.sms.ws

import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Waits for an `Ack` frame that correlates to an outbound `SendSms.id`.
 *
 * The WebSocketSmsGatewayClient puts a future here keyed by `commandId`
 * before sending the frame; the handler completes it when the matching
 * `ack` arrives. Times out via `get(timeout)`.
 */
@Component
class PendingAckRegistry {
    private val waiting = ConcurrentHashMap<String, CompletableFuture<WsFrame.Ack>>()

    fun await(commandId: String): CompletableFuture<WsFrame.Ack> {
        val fut = CompletableFuture<WsFrame.Ack>()
        waiting[commandId] = fut
        fut.whenComplete { _, _ -> waiting.remove(commandId) }
        return fut
    }

    fun complete(ack: WsFrame.Ack) {
        val correlationId = ack.correlationId ?: return
        waiting.remove(correlationId)?.complete(ack)
    }

    fun fail(commandId: String, reason: String) {
        waiting.remove(commandId)?.completeExceptionally(RuntimeException(reason))
    }
}
