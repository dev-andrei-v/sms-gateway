package ro.andreidev.sms.middleware.sms

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

/**
 * Wraps the Android SmsManager and wires sent / delivered PendingIntents so
 * broadcast receivers can translate platform results back into WS status frames.
 *
 * Long messages are automatically split via divideMessage() and each part gets
 * its own intent; status is emitted once for the whole send using the last part's
 * outcome (Android convention for composite SMS).
 */
class SmsSender(private val context: Context) {

    fun send(providerMessageId: String, phoneNumber: String, content: String, simSlot: Int?): Boolean {
        val manager = resolveManager(simSlot) ?: run {
            Log.e(TAG, "no SmsManager for simSlot=$simSlot")
            return false
        }
        val parts = manager.divideMessage(content)
        val sentIntents = ArrayList<PendingIntent>(parts.size)
        val deliveredIntents = ArrayList<PendingIntent>(parts.size)

        for (i in parts.indices) {
            sentIntents += pendingIntent(
                SmsSentReceiver::class.java, SmsSentReceiver.ACTION, providerMessageId, i, parts.size
            )
            deliveredIntents += pendingIntent(
                SmsDeliveredReceiver::class.java, SmsDeliveredReceiver.ACTION, providerMessageId, i, parts.size
            )
        }
        Log.i(TAG, "dispatching providerMessageId=$providerMessageId parts=${parts.size} to=$phoneNumber sim=$simSlot")
        return try {
            manager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveredIntents)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "SmsManager threw", t)
            false
        }
    }

    /**
     * Build an explicit PendingIntent that targets our receiver by component.
     *
     * Naming the component (rather than relying on action + setPackage) is what
     * allows receivers declared in the manifest *without* an <intent-filter>
     * to receive these callbacks — implicit intents without a filter match are
     * dropped on Android 8+.
     */
    private fun pendingIntent(
        receiverClass: Class<out BroadcastReceiver>,
        action: String,
        providerMessageId: String,
        partIndex: Int,
        partCount: Int
    ): PendingIntent {
        val intent = Intent(context, receiverClass).apply {
            this.action = action
            putExtra(EXTRA_PROVIDER_ID, providerMessageId)
            putExtra(EXTRA_PART_INDEX, partIndex)
            putExtra(EXTRA_PART_COUNT, partCount)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = (providerMessageId.hashCode() * 31 + partIndex) xor receiverClass.name.hashCode()
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    @Suppress("DEPRECATION")
    private fun resolveManager(simSlot: Int?): SmsManager? {
        // On Android 12+, SmsManager is retrievable from Context. We prefer the
        // SubscriptionManager path for multi-SIM when a slot is explicitly chosen.
        if (simSlot != null) {
            val subId = SimManager(context).subscriptionIdForSlot(simSlot)
            if (subId != null) {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                        ?.createForSubscriptionId(subId)
                } else {
                    SmsManager.getSmsManagerForSubscriptionId(subId)
                }
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
    }

    companion object {
        private const val TAG = "SmsSender"
        const val EXTRA_PROVIDER_ID = "provider_id"
        const val EXTRA_PART_INDEX = "part_index"
        const val EXTRA_PART_COUNT = "part_count"
    }
}
