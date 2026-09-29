package com.baselalhabib.personalcontext.core.connectors

import android.Manifest
import android.content.Context
import android.provider.CallLog
import com.baselalhabib.personalcontext.core.entities.CallLogEntity
import com.baselalhabib.personalcontext.core.permissions.PermissionsHelper
import java.util.UUID

/**
 * Connector implementation for call log context entities.
 * Supports both manual/staged call logs and querying Android CallLog provider when [context] is supplied.
 */
class CallLogConnector(
    private val context: Context? = null
) : Connector<CallLogEntity> {

    override val id: String = "call_log_connector"

    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.READ_CALL_LOG
    )

    private val pendingCallLogs = mutableListOf<CallLogEntity>()

    /**
     * Creates and stages a new [CallLogEntity] into the connector's pending queue.
     */
    fun addCallLog(
        number: String,
        duration: Long,
        callType: String = "incoming",
        callerName: String? = null,
        id: String = UUID.randomUUID().toString(),
        timestamp: Long = System.currentTimeMillis(),
        source: String = "call_log"
    ): CallLogEntity {
        val callLog = CallLogEntity(
            id = id,
            timestamp = timestamp,
            source = source,
            number = number,
            duration = duration,
            callType = callType,
            callerName = callerName
        )
        synchronized(pendingCallLogs) {
            pendingCallLogs.add(callLog)
        }
        return callLog
    }

    /**
     * Stages an existing [CallLogEntity] into the connector's pending queue.
     */
    fun addCallLog(callLog: CallLogEntity) {
        synchronized(pendingCallLogs) {
            pendingCallLogs.add(callLog)
        }
    }

    /**
     * Collects call logs from both the pending queue and the device's CallLog provider (if [context] is present
     * and permission granted). Clears collected pending call logs.
     */
    override suspend fun collect(sinceTimestamp: Long?): List<CallLogEntity> {
        val results = mutableListOf<CallLogEntity>()

        synchronized(pendingCallLogs) {
            val collectedPending = if (sinceTimestamp != null) {
                pendingCallLogs.filter { it.timestamp >= sinceTimestamp }
            } else {
                pendingCallLogs.toList()
            }
            results.addAll(collectedPending)
            pendingCallLogs.removeAll(collectedPending)
        }

        context?.let { ctx ->
            if (PermissionsHelper.isPermissionGranted(ctx, Manifest.permission.READ_CALL_LOG)) {
                results.addAll(queryCallLogProvider(ctx, sinceTimestamp))
            }
        }

        return results
    }

    private fun queryCallLogProvider(ctx: Context, sinceTimestamp: Long?): List<CallLogEntity> {
        val callLogs = mutableListOf<CallLogEntity>()
        val uri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )

        val selection = if (sinceTimestamp != null) {
            "${CallLog.Calls.DATE} >= ?"
        } else null

        val selectionArgs = if (sinceTimestamp != null) {
            arrayOf(sinceTimestamp.toString())
        } else null

        val sortOrder = "${CallLog.Calls.DATE} DESC"

        try {
            ctx.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(CallLog.Calls._ID)
                val numberColumn = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameColumn = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val dateColumn = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durationColumn = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val typeColumn = cursor.getColumnIndex(CallLog.Calls.TYPE)

                while (cursor.moveToNext()) {
                    val logId = if (idColumn != -1) cursor.getString(idColumn) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                    val number = if (numberColumn != -1) cursor.getString(numberColumn) ?: "Unknown" else "Unknown"
                    val name = if (nameColumn != -1) cursor.getString(nameColumn) else null
                    val date = if (dateColumn != -1) cursor.getLong(dateColumn) else System.currentTimeMillis()
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    val rawType = if (typeColumn != -1) cursor.getInt(typeColumn) else CallLog.Calls.INCOMING_TYPE

                    val typeStr = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> "incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                        CallLog.Calls.MISSED_TYPE -> "missed"
                        CallLog.Calls.VOICEMAIL_TYPE -> "voicemail"
                        CallLog.Calls.REJECTED_TYPE -> "rejected"
                        CallLog.Calls.BLOCKED_TYPE -> "blocked"
                        else -> "unknown"
                    }

                    callLogs.add(
                        CallLogEntity(
                            id = "call_$logId",
                            timestamp = date,
                            source = "call_log",
                            number = number,
                            duration = duration,
                            callType = typeStr,
                            callerName = name
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Return collected so far if query fails
        }

        return callLogs
    }

    /**
     * Returns a snapshot of all currently pending call logs without clearing them.
     */
    fun getPendingCallLogs(): List<CallLogEntity> {
        synchronized(pendingCallLogs) {
            return pendingCallLogs.toList()
        }
    }

    /**
     * Clears all pending call logs.
     */
    fun clearPendingCallLogs() {
        synchronized(pendingCallLogs) {
            pendingCallLogs.clear()
        }
    }
}
