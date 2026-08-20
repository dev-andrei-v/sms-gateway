package ro.andreidev.sms.middleware.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OutboxDao {

    @Insert
    suspend fun enqueue(entity: OutboxEntity): Long

    @Query("SELECT * FROM outbox ORDER BY id ASC LIMIT :limit")
    suspend fun peek(limit: Int = 50): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("UPDATE outbox SET attempts = attempts + 1, lastAttemptAt = :now WHERE id = :id")
    suspend fun markAttempted(id: Long, now: Long)

    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun size(): Int
}
