package ro.andreidev.sms.middleware.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ro.andreidev.sms.middleware.MainActivity
import ro.andreidev.sms.middleware.R
import ro.andreidev.sms.middleware.data.Settings
import ro.andreidev.sms.middleware.data.db.AppDatabase
import ro.andreidev.sms.middleware.data.db.MessageEntity
import ro.andreidev.sms.middleware.sms.SmsSender
import ro.andreidev.sms.middleware.sms.SmsStatusBus
import ro.andreidev.sms.middleware.ws.SmsEvents
import ro.andreidev.sms.middleware.ws.WsClient
import ro.andreidev.sms.middleware.ws.WsConnectionState
import ro.andreidev.sms.middleware.ws.WsFrame
import java.util.UUID

/**
 * Long-running foreground service that owns the WebSocket session.
 *
 * Lifecycle:
 *  - START_STICKY so Android restarts us if killed.
 *  - On create: build notification channel, start WsClient, subscribe to the
 *    SmsStatusBus and forward every event to the backend as an SmsStatus or
 *    SmsReceived frame.
 *  - On destroy: cleanly shut down the WsClient.
 */
class GatewayForegroundService : LifecycleService() {

    private lateinit var settings: Settings
    private lateinit var sender: SmsSender
    private lateinit var wsClient: WsClient

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        settings = Settings(applicationContext)
        sender = SmsSender(applicationContext)

        createChannel()
        startForegroundCompat(buildNotification(connected = false))

        lifecycleScope.launch {
            settings.ensureDeviceId()
            wsClient = WsClient(applicationContext, settings, ::onFrame)
            wsClient.start()
            observeWsState()
            observeSmsEvents()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        _isRunning.value = false
        if (::wsClient.isInitialized) wsClient.shutdown()
        super.onDestroy()
    }

    private suspend fun onFrame(frame: WsFrame) {
        Log.i(TAG, "received frame type=${frame::class.simpleName} id=${frame.id} corr=${frame.correlationId}")
        when (frame) {
            is WsFrame.SendSms -> handleSendCommand(frame)
            is WsFrame.DeviceRegisterAck -> Log.i(TAG, "register ack accepted=${frame.accepted} reason=${frame.reason}")
            is WsFrame.Ping -> wsClient.send(WsFrame.Pong(id = UUID.randomUUID().toString(), correlationId = frame.id))
            else -> Unit
        }
    }

    private suspend fun handleSendCommand(cmd: WsFrame.SendSms) {
        val snap = settings.snapshot()
        Log.i(TAG, "send_sms deviceId=${snap.deviceId} providerMessageId=${cmd.providerMessageId} to=${cmd.phoneNumber} sim=${cmd.simSlot} length=${cmd.content.length}")
        val db = AppDatabase.get(applicationContext)
        val now = Clock.System.now().toEpochMilliseconds()
        db.messages().upsert(
            MessageEntity(
                providerMessageId = cmd.providerMessageId,
                direction = MessageEntity.DIRECTION_OUT,
                phoneNumber = cmd.phoneNumber,
                content = cmd.content,
                status = MessageEntity.STATUS_PENDING,
                simSlot = cmd.simSlot,
                createdAt = now,
                updatedAt = now
            )
        )
        // Ack the queued state immediately so the backend can mark PENDING.
        wsClient.send(
            WsFrame.Ack(
                id = UUID.randomUUID().toString(),
                correlationId = cmd.id,
                deviceId = snap.deviceId,
                providerMessageId = cmd.providerMessageId,
                state = "PENDING"
            )
        )
        val dispatched = sender.send(cmd.providerMessageId, cmd.phoneNumber, cmd.content, cmd.simSlot)
        Log.i(TAG, "SmsManager dispatch returned $dispatched for providerMessageId=${cmd.providerMessageId}")
        if (!dispatched) {
            db.messages().updateStatus(
                cmd.providerMessageId, MessageEntity.STATUS_FAILED, "send_dispatch_failed", now
            )
            wsClient.send(
                WsFrame.SmsStatus(
                    id = UUID.randomUUID().toString(),
                    deviceId = snap.deviceId,
                    providerMessageId = cmd.providerMessageId,
                    event = SmsEvents.FAILED,
                    reason = "send_dispatch_failed",
                    occurredAt = Clock.System.now().toString()
                )
            )
        }
    }

    private fun observeSmsEvents() {
        SmsStatusBus.events.onEach { ev ->
            val snap = settings.snapshot()
            val db = AppDatabase.get(applicationContext)
            val now = Clock.System.now()
            val nowMs = now.toEpochMilliseconds()
            when (ev) {
                is SmsStatusBus.Event.Sent -> {
                    // Emit status only when the final part reports.
                    if (ev.partIndex != ev.partCount - 1) {
                        Log.d(TAG, "intermediate sent part ${ev.partIndex}/${ev.partCount} providerMessageId=${ev.providerMessageId}")
                        return@onEach
                    }
                    val event = if (ev.resultCode == android.app.Activity.RESULT_OK) SmsEvents.SENT else SmsEvents.FAILED
                    val status = if (event == SmsEvents.SENT) MessageEntity.STATUS_SENT else MessageEntity.STATUS_FAILED
                    Log.i(TAG, "forwarding sent-status deviceId=${snap.deviceId} providerMessageId=${ev.providerMessageId} event=$event reason=${ev.reason}")
                    db.messages().updateStatus(ev.providerMessageId, status, ev.reason, nowMs)
                    wsClient.send(
                        WsFrame.SmsStatus(
                            id = UUID.randomUUID().toString(),
                            deviceId = snap.deviceId,
                            providerMessageId = ev.providerMessageId,
                            event = event,
                            reason = ev.reason,
                            occurredAt = now.toString()
                        )
                    )
                }
                is SmsStatusBus.Event.Delivered -> {
                    if (ev.partIndex != ev.partCount - 1) {
                        Log.d(TAG, "intermediate delivered part ${ev.partIndex}/${ev.partCount} providerMessageId=${ev.providerMessageId}")
                        return@onEach
                    }
                    val event = if (ev.resultCode == android.app.Activity.RESULT_OK) SmsEvents.DELIVERED else SmsEvents.FAILED
                    val status = if (event == SmsEvents.DELIVERED) MessageEntity.STATUS_DELIVERED else MessageEntity.STATUS_FAILED
                    Log.i(TAG, "forwarding delivered-status deviceId=${snap.deviceId} providerMessageId=${ev.providerMessageId} event=$event reason=${ev.reason}")
                    db.messages().updateStatus(ev.providerMessageId, status, ev.reason, nowMs)
                    wsClient.send(
                        WsFrame.SmsStatus(
                            id = UUID.randomUUID().toString(),
                            deviceId = snap.deviceId,
                            providerMessageId = ev.providerMessageId,
                            event = event,
                            reason = ev.reason,
                            occurredAt = now.toString()
                        )
                    )
                }
                is SmsStatusBus.Event.Received -> {
                    if (!snap.forwardIncoming) {
                        Log.d(TAG, "forwardIncoming=false, dropping inbound from=${ev.fromNumber}")
                        return@onEach
                    }
                    Log.i(TAG, "forwarding inbound deviceId=${snap.deviceId} from=${ev.fromNumber} length=${ev.content.length}")
                    db.messages().upsert(
                        MessageEntity(
                            providerMessageId = ev.providerMessageId,
                            direction = MessageEntity.DIRECTION_IN,
                            phoneNumber = ev.fromNumber,
                            content = ev.content,
                            status = MessageEntity.STATUS_RECEIVED,
                            simSlot = ev.simSlot,
                            createdAt = ev.receivedAt,
                            updatedAt = nowMs
                        )
                    )
                    wsClient.send(
                        WsFrame.SmsReceived(
                            id = UUID.randomUUID().toString(),
                            deviceId = snap.deviceId,
                            fromNumber = ev.fromNumber,
                            content = ev.content,
                            receivedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(ev.receivedAt).toString(),
                            simSlot = ev.simSlot
                        )
                    )
                }
            }
        }.launchIn(lifecycleScope)
    }

    private fun observeWsState() {
        lifecycleScope.launch {
            wsClient.state.collectLatest { state ->
                val connected = state == WsConnectionState.AUTHENTICATED
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(connected))
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(connected: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setContentTitle(getString(R.string.notification_running_title))
            .setContentText(
                if (connected) getString(R.string.notification_running_text)
                else getString(R.string.notification_offline_text)
            )
            .setSmallIcon(R.drawable.ic_notification_service)
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val TAG = "GatewayService"
        private const val NOTIFICATION_ID = 42
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, GatewayForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GatewayForegroundService::class.java))
        }
    }
}
