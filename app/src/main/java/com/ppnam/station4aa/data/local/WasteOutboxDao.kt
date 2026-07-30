package com.ppnam.station4aa.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WasteOutboxDao {

    // IGNORE, not REPLACE: messageId is the primary key and the event is immutable once created
    // (contract: reuse the same messageId/payload on every retry). A second insert of the same
    // messageId is exactly that retry path re-queuing, not a new event to replace the old row.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WasteOutboxEntity)

    @Query("SELECT * FROM waste_outbox WHERE status = 'PENDING' ORDER BY createdAtEpochMs ASC")
    suspend fun getPending(): List<WasteOutboxEntity>

    @Query("SELECT COUNT(*) FROM waste_outbox WHERE status = 'PENDING'")
    fun pendingCount(): Flow<Int>

    @Query(
        "UPDATE waste_outbox SET attemptCount = attemptCount + 1, lastAttemptEpochMs = :nowEpochMs " +
            "WHERE messageId = :messageId"
    )
    suspend fun recordAttempt(messageId: String, nowEpochMs: Long)

    @Query("UPDATE waste_outbox SET status = 'DELIVERED' WHERE messageId = :messageId")
    suspend fun markDelivered(messageId: String)
}
