package ro.andreidev.sms.middleware.sms

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

/**
 * Multi-SIM helper. All methods silently return nulls/empty lists if
 * READ_PHONE_STATE is not granted, so callers don't need to branch on permission.
 */
class SimManager(private val context: Context) {

    data class SimInfo(val slotIndex: Int, val subscriptionId: Int, val carrierName: String, val displayName: String)

    @SuppressLint("MissingPermission")
    fun activeSims(): List<SimInfo> {
        if (!hasPermission()) return emptyList()
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return emptyList()
        return sm.activeSubscriptionInfoList.orEmpty().map {
            SimInfo(
                slotIndex = it.simSlotIndex,
                subscriptionId = it.subscriptionId,
                carrierName = it.carrierName?.toString().orEmpty(),
                displayName = it.displayName?.toString().orEmpty()
            )
        }
    }

    fun subscriptionIdForSlot(slotIndex: Int): Int? =
        activeSims().firstOrNull { it.slotIndex == slotIndex }?.subscriptionId

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
}
