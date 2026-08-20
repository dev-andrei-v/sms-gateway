package ro.andreidev.sms.middleware.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local record of every message the device has processed, indexed by the
 * backend-assigned providerMessageId for fast correlation with status updates.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["providerMessageId"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerMessageId: String,
    val direction: String,
    val phoneNumber: String,
    val content: String,
    val status: String,
    val simSlot: Int? = null,
    val reason: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val DIRECTION_OUT = "OUT"
        const val DIRECTION_IN = "IN"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_DELIVERED = "DELIVERED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_RECEIVED = "RECEIVED"
    }
}
