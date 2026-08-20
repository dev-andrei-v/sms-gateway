package ro.andreidev.sms.middleware.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val providerMessageId = intent.getStringExtra(SmsSender.EXTRA_PROVIDER_ID) ?: run {
            Log.w("SmsDeliveredReceiver", "received intent without provider id: ${intent.action}")
            return
        }
        val partIndex = intent.getIntExtra(SmsSender.EXTRA_PART_INDEX, 0)
        val partCount = intent.getIntExtra(SmsSender.EXTRA_PART_COUNT, 1)
        val reason = if (resultCode == Activity.RESULT_OK) null else "delivery_failed"
        Log.i("SmsDeliveredReceiver", "delivered resultCode=$resultCode providerMessageId=$providerMessageId part=$partIndex/$partCount")
        SmsStatusBus.emit(
            SmsStatusBus.Event.Delivered(providerMessageId, partIndex, partCount, resultCode, reason)
        )
    }

    companion object {
        const val ACTION = "ro.andreidev.sms.middleware.SMS_DELIVERED"
    }
}
