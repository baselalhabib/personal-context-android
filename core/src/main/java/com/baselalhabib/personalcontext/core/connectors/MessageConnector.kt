package com.baselalhabib.personalcontext.core.connectors

import android.Manifest
import android.content.Context
import android.provider.Telephony
import com.baselalhabib.personalcontext.core.entities.MessageEntity
import com.baselalhabib.personalcontext.core.permissions.PermissionsHelper
import java.util.UUID

/**
 * Connector implementation for message context entities (SMS).
 * Supports both manual/staged messages and pulling SMS from Android Telephony provider when [context] is supplied.
 */
class MessageConnector(
    private val context: Context? = null
) : Connector<MessageEntity> {

    override val id: String = "message_connector"

    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.READ_SMS
    )

    private val pendingMessages = mutableListOf<MessageEntity>()

    /**
     * Creates and stages a new [MessageEntity] into the connector's pending queue.
     */
    fun addMessage(
        sender: String,
        body: String,
        type: String = "inbox",
        id: String = UUID.randomUUID().toString(),
        timestamp: Long = System.currentTimeMillis(),
        source: String = "sms"
    ): MessageEntity {
        val message = MessageEntity(
            id = id,
            timestamp = timestamp,
            source = source,
            sender = sender,
            body = body,
            type = type
        )
        synchronized(pendingMessages) {
            pendingMessages.add(message)
        }
        return message
    }

    /**
     * Stages an existing [MessageEntity] into the connector's pending queue.
     */
    fun addMessage(message: MessageEntity) {
        synchronized(pendingMessages) {
            pendingMessages.add(message)
        }
    }

    /**
     * Collects messages from both the pending queue and the device's SMS provider (if [context] is present
     * and permission granted). Clears collected pending messages.
     */
    override suspend fun collect(sinceTimestamp: Long?): List<MessageEntity> {
        val results = mutableListOf<MessageEntity>()

        synchronized(pendingMessages) {
            val collectedPending = if (sinceTimestamp != null) {
                pendingMessages.filter { it.timestamp >= sinceTimestamp }
            } else {
                pendingMessages.toList()
            }
            results.addAll(collectedPending)
            pendingMessages.removeAll(collectedPending)
        }

        context?.let { ctx ->
            if (PermissionsHelper.isPermissionGranted(ctx, Manifest.permission.READ_SMS)) {
                results.addAll(querySmsProvider(ctx, sinceTimestamp))
            }
        }

        return results
    }

    private fun querySmsProvider(ctx: Context, sinceTimestamp: Long?): List<MessageEntity> {
        val messages = mutableListOf<MessageEntity>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val selection = if (sinceTimestamp != null) {
            "${Telephony.Sms.DATE} >= ?"
        } else null

        val selectionArgs = if (sinceTimestamp != null) {
            arrayOf(sinceTimestamp.toString())
        } else null

        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            ctx.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(Telephony.Sms._ID)
                val addressColumn = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyColumn = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateColumn = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeColumn = cursor.getColumnIndex(Telephony.Sms.TYPE)

                while (cursor.moveToNext()) {
                    val smsId = if (idColumn != -1) cursor.getString(idColumn) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                    val address = if (addressColumn != -1) cursor.getString(addressColumn) ?: "Unknown" else "Unknown"
                    val body = if (bodyColumn != -1) cursor.getString(bodyColumn) ?: "" else ""
                    val date = if (dateColumn != -1) cursor.getLong(dateColumn) else System.currentTimeMillis()
                    val rawType = if (typeColumn != -1) cursor.getInt(typeColumn) else Telephony.Sms.MESSAGE_TYPE_INBOX

                    val typeStr = when (rawType) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> "inbox"
                        Telephony.Sms.MESSAGE_TYPE_SENT -> "sent"
                        Telephony.Sms.MESSAGE_TYPE_DRAFT -> "draft"
                        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "outbox"
                        else -> "unknown"
                    }

                    messages.add(
                        MessageEntity(
                            id = "sms_$smsId",
                            timestamp = date,
                            source = "sms",
                            sender = address,
                            body = body,
                            type = typeStr
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Return collected so far if query fails
        }

        return messages
    }

    /**
     * Returns a snapshot of all currently pending messages without clearing them.
     */
    fun getPendingMessages(): List<MessageEntity> {
        synchronized(pendingMessages) {
            return pendingMessages.toList()
        }
    }

    /**
     * Clears all pending messages.
     */
    fun clearPendingMessages() {
        synchronized(pendingMessages) {
            pendingMessages.clear()
        }
    }
}
