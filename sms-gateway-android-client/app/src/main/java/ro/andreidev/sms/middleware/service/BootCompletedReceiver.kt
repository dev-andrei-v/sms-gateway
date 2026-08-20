package ro.andreidev.sms.middleware.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ro.andreidev.sms.middleware.data.Settings

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in START_ACTIONS) return
        val appContext = context.applicationContext
        val settings = Settings(appContext)

        val cached = settings.cachedSnapshot()
        if (cached.isConfigured && cached.autoStart) {
            Log.i(TAG, "starting service from cached boot settings for action=$action")
            GatewayForegroundService.start(appContext)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snap = settings.snapshot()
                if (snap.isConfigured && snap.autoStart) {
                    Log.i(TAG, "starting service after datastore boot check for action=$action")
                    GatewayForegroundService.start(appContext)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "boot receiver failed for action=$action", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"

        private val START_ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_UNLOCKED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )
    }
}
