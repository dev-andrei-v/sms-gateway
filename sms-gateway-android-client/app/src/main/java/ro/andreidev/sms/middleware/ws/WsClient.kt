package ro.andreidev.sms.middleware.ws

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import ro.andreidev.sms.middleware.data.Settings
import ro.andreidev.sms.middleware.data.db.AppDatabase
import ro.andreidev.sms.middleware.data.db.OutboxEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * Persistent WebSocket link between the Android middleware and the backend.
 *
 * Contract:
 *  - Opens an HTTP(S) upgrade request to `<serverUrl>/ws/device` with
 *    `Authorization: Bearer <apiKey>`.
 *  - Sends `DeviceRegister` as the first frame; considers the session
 *    authenticated once `DeviceRegisterAck(accepted=true)` arrives.
 *  - Delivers every inbound frame to [onFrame] on the client's coroutine scope.
 *  - Every outbound frame goes through [send], which persists to the Outbox
 *    first; the Outbox is drained on (re)connect, so messages survive
 *    disconnects and process restarts.
 *  - Reconnects on any failure with exponential backoff (1s → 32s cap), and
 *    heartbeats every 25s.
 */
class WsClient(
    private val context: Context,
    private val settings: Settings,
    private val onFrame: suspend (WsFrame) -> Unit
) {
    private val tag = "WsClient"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _state = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val state: StateFlow<WsConnectionState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    @Volatile private var socket: WebSocket? = null
    private var loopJob: Job? = null
    private var heartbeatJob: Job? = null

    fun start() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch { connectionLoop() }
    }

    fun stop() {
        loopJob?.cancel()
        heartbeatJob?.cancel()
        socket?.close(1000, "client_stop")
        socket = null
        _state.value = WsConnectionState.DISCONNECTED
    }

    /** Persist + attempt immediate send. Survives disconnect via the Outbox. */
    suspend fun send(frame: WsFrame) {
        val payload = WsJson.json.encodeToString<WsFrame>(frame)
        val outboxId = AppDatabase.get(context).outbox().enqueue(
            OutboxEntity(payload = payload, enqueuedAt = Clock.System.now().toEpochMilliseconds())
        )
        Log.i(tag, "enqueue outbound type=${frame::class.simpleName} id=${frame.id} corr=${frame.correlationId} socketOpen=${socket != null}")
        tryDrain(outboxId to payload)
    }

    private suspend fun tryDrain(primed: Pair<Long, String>? = null) {
        val ws = socket ?: return
        val outbox = AppDatabase.get(context).outbox()
        primed?.let { (id, payload) ->
            if (ws.send(payload)) {
                outbox.remove(id)
            } else {
                outbox.markAttempted(id, Clock.System.now().toEpochMilliseconds())
                return
            }
        }
        while (true) {
            val batch = outbox.peek(limit = 20)
            if (batch.isEmpty()) return
            for (entry in batch) {
                val sent = ws.send(entry.payload)
                if (sent) {
                    outbox.remove(entry.id)
                } else {
                    outbox.markAttempted(entry.id, Clock.System.now().toEpochMilliseconds())
                    return
                }
            }
        }
    }

    private suspend fun connectionLoop() {
        var attempt = 0
        while (scope.isActive) {
            val snap = settings.snapshot()
            if (!snap.isConfigured) {
                _state.value = WsConnectionState.DISCONNECTED
                _lastError.value = "Not configured"
                delay(5_000)
                continue
            }
            _state.value = WsConnectionState.CONNECTING
            val connected = runCatching { openSocket(snap) }
                .onFailure {
                    _lastError.value = it.message
                    Log.w(tag, "openSocket failed", it)
                }
                .getOrDefault(false)
            if (!connected) {
                val backoffMs = (1000.0 * 2.0.pow(min(attempt, 5))).toLong()
                _state.value = WsConnectionState.FAILED
                delay(backoffMs)
                attempt += 1
                continue
            }
            attempt = 0
            // Wait here until the socket closes; the listener resets `socket` to null.
            while (scope.isActive && socket != null) delay(500)
            heartbeatJob?.cancel()
            _state.value = WsConnectionState.DISCONNECTED
            delay(1_000)
        }
    }

    private suspend fun openSocket(snap: Settings.Snapshot): Boolean {
        val url = toWsUrl(snap.serverUrl)
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${snap.apiKey}")
            .addHeader("X-Device-Id", snap.deviceId)
            .build()

        val opened = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socket = webSocket
                _state.value = WsConnectionState.CONNECTED
                _lastError.value = null
                opened.complete(true)
                scope.launch { registerAndDrain(snap) }
                startHeartbeat()
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch { handleInbound(text) }
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                scope.launch { handleInbound(bytes.utf8()) }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "ws failure: ${t.message}")
                _lastError.value = t.message
                socket = null
                if (!opened.isCompleted) opened.complete(false)
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(tag, "ws closing: $code $reason")
                webSocket.close(1000, null)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(tag, "ws closed: $code $reason")
                socket = null
            }
        }
        http.newWebSocket(req, listener)
        return opened.await()
    }

    private suspend fun registerAndDrain(snap: Settings.Snapshot) {
        val register = WsFrame.DeviceRegister(
            id = UUID.randomUUID().toString(),
            deviceId = snap.deviceId,
            deviceName = snap.deviceName,
            osVersion = android.os.Build.VERSION.RELEASE,
            appVersion = "0.1.0"
        )
        socket?.send(WsJson.json.encodeToString<WsFrame>(register))
        tryDrain()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && socket != null) {
                delay(25_000)
                val ping = WsFrame.Ping(id = UUID.randomUUID().toString())
                socket?.send(WsJson.json.encodeToString<WsFrame>(ping))
            }
        }
    }

    private suspend fun handleInbound(raw: String) {
        val frame = runCatching { WsJson.json.decodeFromString<WsFrame>(raw) }
            .onFailure { Log.w(tag, "bad frame: $raw", it) }
            .getOrNull() ?: return
        Log.i(tag, "inbound type=${frame::class.simpleName} id=${frame.id} corr=${frame.correlationId}")
        if (frame is WsFrame.DeviceRegisterAck && frame.accepted) {
            _state.value = WsConnectionState.AUTHENTICATED
        }
        onFrame(frame)
    }

    private fun toWsUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        val swapped = when {
            trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
            trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
            trimmed.startsWith("ws://") || trimmed.startsWith("wss://") -> trimmed
            else -> "wss://$trimmed"
        }
        return "$swapped/ws/device"
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }
}
