package ro.andreidev.sms.middleware.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE providerMessageId = :providerMessageId LIMIT 1")
    suspend fun findByProviderId(providerMessageId: String): MessageEntity?

    @Query("UPDATE messages SET status = :status, reason = :reason, updatedAt = :updatedAt WHERE providerMessageId = :providerMessageId")
    suspend fun updateStatus(providerMessageId: String, status: String, reason: String?, updatedAt: Long)

    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages")
    suspend fun clear()
}
