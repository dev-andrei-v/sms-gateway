package ro.andreidev.sms.middleware.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val providerMessageId = intent.getStringExtra(SmsSender.EXTRA_PROVIDER_ID) ?: run {
            Log.w("SmsSentReceiver", "received intent without provider id: ${intent.action}")
            return
        }
        val partIndex = intent.getIntExtra(SmsSender.EXTRA_PART_INDEX, 0)
        val partCount = intent.getIntExtra(SmsSender.EXTRA_PART_COUNT, 1)
        val reason = decodeReason(resultCode)
        Log.i("SmsSentReceiver", "sent resultCode=$resultCode providerMessageId=$providerMessageId part=$partIndex/$partCount")
        SmsStatusBus.emit(
            SmsStatusBus.Event.Sent(providerMessageId, partIndex, partCount, resultCode, reason)
        )
    }

    private fun decodeReason(code: Int): String? = when (code) {
        Activity.RESULT_OK -> null
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "generic_failure"
        SmsManager.RESULT_ERROR_NO_SERVICE -> "no_service"
        SmsManager.RESULT_ERROR_NULL_PDU -> "null_pdu"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "radio_off"
        else -> "error_$code"
    }

    companion object {
        const val ACTION = "ro.andreidev.sms.middleware.SMS_SENT"
    }
}
