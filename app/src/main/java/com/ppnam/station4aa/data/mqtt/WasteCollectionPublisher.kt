package com.ppnam.station4aa.data.mqtt

import com.google.gson.Gson
import com.ppnam.station4aa.data.local.WasteOutboxDao
import com.ppnam.station4aa.data.local.toEvent
import com.ppnam.station4aa.data.local.toOutboxEntity
import com.ppnam.station4aa.domain.model.WasteCollectionEvent
import kotlinx.coroutines.flow.Flow
import java.nio.charset.StandardCharsets

/**
 * Implements the handheld side of `C:\Dev\PPNAM-Station-4\DOCS\Station4_Wastage_MQTT_Contract.md`,
 * "Required handheld workflow" steps 9–11: durably write before the first publish attempt, only
 * clear the interactive transaction after that durable write, and only count an event delivered
 * once the scanner receives PUBACK — never sooner, and never as proof Station 4 accepted it (see
 * MqttConnectionManager's class doc on what a PUBACK does and doesn't mean here).
 */
class WasteCollectionPublisher(
    private val outboxDao: WasteOutboxDao,
    private val connectionManager: MqttConnectionManager,
) {
    private val gson = Gson()

    val pendingCount: Flow<Int> = outboxDao.pendingCount()

    /**
     * Durably queues [event], then makes one publish attempt. Returns once the row is safely on
     * disk — callers can clear their interactive form the moment this returns, regardless of
     * whether the immediate publish attempt (best-effort) succeeded, per step 10: "clear the
     * interactive transaction only after the durable local write" (not after delivery).
     */
    suspend fun submit(event: WasteCollectionEvent) {
        outboxDao.insert(event.toOutboxEntity(System.currentTimeMillis()))
        attemptPublish(event)
    }

    /** Retries every durably-queued, not-yet-PUBACKed event with its original, unchanged payload
     * — call after a reconnect so anything queued while offline gets flushed. */
    suspend fun retryPending() {
        outboxDao.getPending().forEach { attemptPublish(it.toEvent()) }
    }

    private suspend fun attemptPublish(event: WasteCollectionEvent) {
        val payload = gson.toJson(event.toWireMessage()).toByteArray(StandardCharsets.UTF_8)
        val result = connectionManager.publish(MqttTopics.WASTE_COLLECTION, payload)
        outboxDao.recordAttempt(event.messageId, System.currentTimeMillis())
        if (result.isSuccess) {
            outboxDao.markDelivered(event.messageId)
        }
    }
}
