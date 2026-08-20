package ro.andreidev.sms.middleware.sms

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide hot channel for SMS lifecycle events emitted by the platform
 * broadcast receivers. The foreground service subscribes and forwards each
 * event to the WebSocket as an SmsStatus frame.
 *
 * Using a process-global so broadcast receivers (which may run in a different
 * process instance when the app is cold) can still deliver into the running
 * service via rebind.
 */
object SmsStatusBus {
    sealed interface Event {
        val providerMessageId: String
        val partIndex: Int
        val partCount: Int

        data class Sent(
            override val providerMessageId: String,
            override val partIndex: Int,
            override val partCount: Int,
            val resultCode: Int,
            val reason: String?
        ) : Event
        data class Delivered(
            override val providerMessageId: String,
            override val partIndex: Int,
            override val partCount: Int,
            val resultCode: Int,
            val reason: String?
        ) : Event
        data class Received(
            override val providerMessageId: String,
            val fromNumber: String,
            val content: String,
            val receivedAt: Long,
            val simSlot: Int?
        ) : Event {
            override val partIndex: Int = 0
            override val partCount: Int = 1
        }
    }

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun emit(event: Event) {
        _events.tryEmit(event)
    }
}
