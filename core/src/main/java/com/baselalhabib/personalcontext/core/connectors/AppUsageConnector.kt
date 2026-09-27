package com.baselalhabib.personalcontext.core.connectors

import android.Manifest
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.baselalhabib.personalcontext.core.entities.AppUsageEntity
import com.baselalhabib.personalcontext.core.permissions.PermissionsHelper
import java.util.UUID

/**
 * Connector implementation for app usage statistics context entities.
 * Supports both manual/staged usage entries and pulling system usage stats from
 * [UsageStatsManager] when [context] is supplied and usage access permission is granted.
 */
class AppUsageConnector(
    private val context: Context? = null,
) : Connector<AppUsageEntity> {

    override val id: String = "app_usage_connector"

    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.PACKAGE_USAGE_STATS
    )

    private val pendingAppUsages = mutableListOf<AppUsageEntity>()

    /**
     * Creates and stages a new [AppUsageEntity] into the connector's pending queue.
     */
    fun addAppUsage(
        packageName: String,
        totalTimeInForeground: Long,
        lastTimeUsed: Long,
        id: String = UUID.randomUUID().toString(),
        timestamp: Long = System.currentTimeMillis(),
        source: String = "usage_stats"
    ): AppUsageEntity {
        val appUsage = AppUsageEntity(
            id = id,
            timestamp = timestamp,
            source = source,
            packageName = packageName,
            totalTimeInForeground = totalTimeInForeground,
            lastTimeUsed = lastTimeUsed
        )
        synchronized(pendingAppUsages) {
            pendingAppUsages.add(appUsage)
        }
        return appUsage
    }

    /**
     * Stages an existing [AppUsageEntity] into the connector's pending queue.
     */
    fun addAppUsage(appUsage: AppUsageEntity) {
        synchronized(pendingAppUsages) {
            pendingAppUsages.add(appUsage)
        }
    }

    /**
     * Collects app usage entries from both the pending queue and the system's [UsageStatsManager]
     * (if [context] is present and permission granted). Clears collected pending entries.
     */
    override suspend fun collect(sinceTimestamp: Long?): List<AppUsageEntity> {
        val results = mutableListOf<AppUsageEntity>()

        synchronized(pendingAppUsages) {
            val collectedPending = if (sinceTimestamp != null) {
                pendingAppUsages.filter { it.timestamp >= sinceTimestamp }
            } else {
                pendingAppUsages.toList()
            }
            results.addAll(collectedPending)
            pendingAppUsages.removeAll(collectedPending)
        }

        context?.let { ctx ->
            if (PermissionsHelper.isPermissionGranted(ctx, Manifest.permission.PACKAGE_USAGE_STATS)) {
                results.addAll(queryUsageStats(ctx, sinceTimestamp))
            }
        }

        return results
    }

    @Suppress("MissingPermission")
    private fun queryUsageStats(ctx: Context, sinceTimestamp: Long?): List<AppUsageEntity> {
        val usageStatsManager = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val endTime = System.currentTimeMillis()
        val startTime = sinceTimestamp ?: (endTime - 24 * 60 * 60 * 1000L)

        val statsList: List<UsageStats> = try {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return statsList
            .filter { it.totalTimeInForeground > 0 }
            .map { stats ->
                AppUsageEntity(
                    id = "usage_${stats.packageName}_${stats.lastTimeUsed}",
                    timestamp = stats.lastTimeUsed,
                    source = "usage_stats",
                    packageName = stats.packageName,
                    totalTimeInForeground = stats.totalTimeInForeground,
                    lastTimeUsed = stats.lastTimeUsed
                )
            }
    }

    /**
     * Returns a snapshot of all currently pending app usage entries without clearing them.
     */
    fun getPendingAppUsages(): List<AppUsageEntity> {
        synchronized(pendingAppUsages) {
            return pendingAppUsages.toList()
        }
    }

    /**
     * Clears all pending app usage entries.
     */
    fun clearPendingAppUsages() {
        synchronized(pendingAppUsages) {
            pendingAppUsages.clear()
        }
    }
}
