package com.baselalhabib.personalcontext.core.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey
    override val id: String,
    override val timestamp: Long,
    override val source: String = "call_log",
    val number: String,
    val duration: Long,
    val callType: String, // "incoming", "outgoing", "missed", "voicemail", "rejected", "blocked", "unknown"
    val callerName: String? = null
) : ContextEntity
