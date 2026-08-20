package ro.andreidev.sms.middleware.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import java.util.UUID

/**
 * Picks up incoming SMS and publishes them on [SmsStatusBus]. The foreground
 * service is responsible for forwarding to the backend over WS.
 *
 * Multi-part messages from the same sender arrive as a single broadcast with
 * all PDUs concatenated by Telephony.Sms.Intents, so we join their bodies and
 * emit a single Received event.
 */
class SmsReceivedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent).orEmpty()
        if (messages.isEmpty()) {
            Log.w(TAG, "SMS_RECEIVED with no messages")
            return
        }
        val from = messages.first().displayOriginatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        val receivedAt = messages.first().timestampMillis
        val simSlot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.getIntExtra("slot", -1).takeIf { it >= 0 }
        } else null

        Log.i(TAG, "inbound SMS from=$from length=${body.length} at=$receivedAt sim=$simSlot")
        SmsStatusBus.emit(
            SmsStatusBus.Event.Received(
                providerMessageId = "in-" + UUID.randomUUID().toString(),
                fromNumber = from,
                content = body,
                receivedAt = receivedAt,
                simSlot = simSlot
            )
        )
    }

    companion object { private const val TAG = "SmsReceivedReceiver" }
}
