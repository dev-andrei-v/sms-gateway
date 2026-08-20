package ro.andreidev.sms.middleware.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.UUID

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "middleware_settings")

/**
 * Persistent app configuration. The device id is generated once on first launch
 * and reused for the life of the install; uninstalling resets it (as it should).
 */
class Settings(private val context: Context) {
    private val bootContext: Context = context.createDeviceProtectedStorageContext() ?: context
    private val bootPrefs: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
        bootContext.getSharedPreferences(BOOT_PREFS_NAME, Context.MODE_PRIVATE)
    }

    data class Snapshot(
        val serverUrl: String,
        val apiKey: String,
        val deviceId: String,
        val deviceName: String,
        val autoStart: Boolean,
        val autoStartOnOpen: Boolean,
        val forwardIncoming: Boolean
    ) {
        val isConfigured: Boolean get() = serverUrl.isNotBlank() && apiKey.isNotBlank()
    }

    val flow: Flow<Snapshot> = context.settingsStore.data
        .map { prefs ->
            Snapshot(
                serverUrl = prefs[KEY_SERVER_URL].orEmpty(),
                apiKey = prefs[KEY_API_KEY].orEmpty(),
                deviceId = prefs[KEY_DEVICE_ID] ?: "",
                deviceName = prefs[KEY_DEVICE_NAME] ?: defaultDeviceName(),
                autoStart = prefs[KEY_AUTO_START] ?: true,
                autoStartOnOpen = prefs[KEY_AUTO_START_ON_OPEN] ?: true,
                forwardIncoming = prefs[KEY_FORWARD_INCOMING] ?: true
            )
        }
        .onEach(::writeBootSnapshot)

    suspend fun snapshot(): Snapshot = runCatching { flow.first() }
        .getOrElse { cachedSnapshot() }

    fun cachedSnapshot(): Snapshot = Snapshot(
        serverUrl = bootPrefs.getString(BOOT_KEY_SERVER_URL, "").orEmpty(),
        apiKey = bootPrefs.getString(BOOT_KEY_API_KEY, "").orEmpty(),
        deviceId = bootPrefs.getString(BOOT_KEY_DEVICE_ID, "").orEmpty(),
        deviceName = bootPrefs.getString(BOOT_KEY_DEVICE_NAME, defaultDeviceName()).orEmpty(),
        autoStart = bootPrefs.getBoolean(BOOT_KEY_AUTO_START, true),
        autoStartOnOpen = bootPrefs.getBoolean(BOOT_KEY_AUTO_START_ON_OPEN, true),
        forwardIncoming = bootPrefs.getBoolean(BOOT_KEY_FORWARD_INCOMING, true)
    )

    suspend fun ensureDeviceId(): String {
        val cached = bootPrefs.getString(BOOT_KEY_DEVICE_ID, null)
        if (!cached.isNullOrBlank()) return cached
        if (!isUserUnlocked()) {
            val generated = UUID.randomUUID().toString()
            bootPrefs.edit().putString(BOOT_KEY_DEVICE_ID, generated).apply()
            return generated
        }
        val current = context.settingsStore.data.first()[KEY_DEVICE_ID]
        if (!current.isNullOrBlank()) return current
        val generated = UUID.randomUUID().toString()
        context.settingsStore.edit { it[KEY_DEVICE_ID] = generated }
        bootPrefs.edit().putString(BOOT_KEY_DEVICE_ID, generated).apply()
        return generated
    }

    suspend fun update(
        serverUrl: String? = null,
        apiKey: String? = null,
        deviceName: String? = null,
        autoStart: Boolean? = null,
        autoStartOnOpen: Boolean? = null,
        forwardIncoming: Boolean? = null
    ) {
        context.settingsStore.edit { prefs ->
            serverUrl?.let { prefs[KEY_SERVER_URL] = it.trim() }
            apiKey?.let { prefs[KEY_API_KEY] = it.trim() }
            deviceName?.let { prefs[KEY_DEVICE_NAME] = it.trim() }
            autoStart?.let { prefs[KEY_AUTO_START] = it }
            autoStartOnOpen?.let { prefs[KEY_AUTO_START_ON_OPEN] = it }
            forwardIncoming?.let { prefs[KEY_FORWARD_INCOMING] = it }
        }
        writeBootSnapshot(snapshot())
    }

    private fun defaultDeviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}"

    private fun isUserUnlocked(): Boolean {
        val userManager = context.getSystemService(UserManager::class.java)
        return userManager?.isUserUnlocked ?: true
    }

    private fun writeBootSnapshot(snapshot: Snapshot) {
        bootPrefs.edit()
            .putString(BOOT_KEY_SERVER_URL, snapshot.serverUrl)
            .putString(BOOT_KEY_API_KEY, snapshot.apiKey)
            .putString(BOOT_KEY_DEVICE_ID, snapshot.deviceId)
            .putString(BOOT_KEY_DEVICE_NAME, snapshot.deviceName)
            .putBoolean(BOOT_KEY_AUTO_START, snapshot.autoStart)
            .putBoolean(BOOT_KEY_AUTO_START_ON_OPEN, snapshot.autoStartOnOpen)
            .putBoolean(BOOT_KEY_FORWARD_INCOMING, snapshot.forwardIncoming)
            .apply()
    }

    companion object {
        private const val BOOT_PREFS_NAME = "middleware_boot_settings"
        private const val BOOT_KEY_SERVER_URL = "server_url"
        private const val BOOT_KEY_API_KEY = "api_key"
        private const val BOOT_KEY_DEVICE_ID = "device_id"
        private const val BOOT_KEY_DEVICE_NAME = "device_name"
        private const val BOOT_KEY_AUTO_START = "auto_start"
        private const val BOOT_KEY_AUTO_START_ON_OPEN = "auto_start_on_open"
        private const val BOOT_KEY_FORWARD_INCOMING = "forward_incoming"
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        private val KEY_AUTO_START = booleanPreferencesKey("auto_start")
        private val KEY_AUTO_START_ON_OPEN = booleanPreferencesKey("auto_start_on_open")
        private val KEY_FORWARD_INCOMING = booleanPreferencesKey("forward_incoming")
    }
}
