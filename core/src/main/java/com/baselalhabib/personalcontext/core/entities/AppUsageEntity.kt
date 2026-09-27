package com.baselalhabib.personalcontext.core.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey
    override val id: String,
    override val timestamp: Long,
    override val source: String = "usage_stats",
    val packageName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
) : ContextEntity
