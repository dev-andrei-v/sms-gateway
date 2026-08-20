package ro.andreidev.sms.middleware.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Frames we need to deliver to the backend but couldn't (offline, WS disconnected).
 * The WsClient drains this queue every time it reconnects.
 */
@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payload: String,
    val enqueuedAt: Long,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null
)
