package com.baselalhabib.personalcontext.core.connectors

import com.baselalhabib.personalcontext.core.entities.NoteEntity
import java.util.UUID

/**
 * Connector implementation for note context entities.
 * Allows staging/creating notes locally and collecting notes filtered by timestamp.
 */
class NoteConnector : Connector<NoteEntity> {

    override val id: String = "note_connector"

    override val requiredPermissions: List<String> = emptyList()

    private val pendingNotes = mutableListOf<NoteEntity>()

    /**
     * Creates and stages a new [NoteEntity] into the connector's pending queue.
     */
    fun addNote(
        title: String,
        content: String,
        id: String = UUID.randomUUID().toString(),
        timestamp: Long = System.currentTimeMillis(),
        source: String = "in_app"
    ): NoteEntity {
        val note = NoteEntity(
            id = id,
            timestamp = timestamp,
            source = source,
            title = title,
            content = content
        )
        synchronized(pendingNotes) {
            pendingNotes.add(note)
        }
        return note
    }

    /**
     * Stages an existing [NoteEntity] into the connector's pending queue.
     */
    fun addNote(note: NoteEntity) {
        synchronized(pendingNotes) {
            pendingNotes.add(note)
        }
    }

    /**
     * Collects notes that match [sinceTimestamp] filtering (or all if null).
     * Clears collected notes from the pending queue.
     */
    override suspend fun collect(sinceTimestamp: Long?): List<NoteEntity> {
        synchronized(pendingNotes) {
            val collected = if (sinceTimestamp != null) {
                pendingNotes.filter { it.timestamp >= sinceTimestamp }
            } else {
                pendingNotes.toList()
            }
            pendingNotes.removeAll(collected)
            return collected
        }
    }

    /**
     * Returns a snapshot of all currently pending notes without clearing them.
     */
    fun getPendingNotes(): List<NoteEntity> {
        synchronized(pendingNotes) {
            return pendingNotes.toList()
        }
    }

    /**
     * Clears all pending notes.
     */
    fun clearPendingNotes() {
        synchronized(pendingNotes) {
            pendingNotes.clear()
        }
    }
}
