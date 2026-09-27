package com.baselalhabib.personalcontext.core.connectors

import android.Manifest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppUsageConnectorTest {

    private lateinit var connector: AppUsageConnector

    @Before
    fun setUp() {
        connector = AppUsageConnector()
    }

    @Test
    fun `id is app_usage_connector and permissions contain PACKAGE_USAGE_STATS`() {
        assertEquals("app_usage_connector", connector.id)
        assertEquals(1, connector.requiredPermissions.size)
        assertEquals(Manifest.permission.PACKAGE_USAGE_STATS, connector.requiredPermissions[0])
    }

    @Test
    fun `addAppUsage adds app usage entry to pending queue`() {
        val appUsage = connector.addAppUsage(
            packageName = "com.example.app",
            totalTimeInForeground = 60000L,
            lastTimeUsed = 100000L,
        )

        assertEquals("com.example.app", appUsage.packageName)
        assertEquals(60000L, appUsage.totalTimeInForeground)
        assertEquals(100000L, appUsage.lastTimeUsed)
        assertEquals("usage_stats", appUsage.source)
        assertEquals(1, connector.getPendingAppUsages().size)
    }

    @Test
    fun `collect returns all pending app usages and clears queue when sinceTimestamp is null`() = runBlocking {
        connector.addAppUsage(packageName = "com.app.one", totalTimeInForeground = 1000L, lastTimeUsed = 5000L)
        connector.addAppUsage(packageName = "com.app.two", totalTimeInForeground = 2000L, lastTimeUsed = 6000L)

        val collected = connector.collect(sinceTimestamp = null)

        assertEquals(2, collected.size)
        assertTrue(connector.getPendingAppUsages().isEmpty())
    }

    @Test
    fun `collect filters by sinceTimestamp`() = runBlocking {
        connector.addAppUsage(packageName = "com.old.app", totalTimeInForeground = 1000L, lastTimeUsed = 5000L, timestamp = 1000L)
        connector.addAppUsage(packageName = "com.new.app", totalTimeInForeground = 2000L, lastTimeUsed = 6000L, timestamp = 2000L)

        val collected = connector.collect(sinceTimestamp = 1500L)

        assertEquals(1, collected.size)
        assertEquals("com.new.app", collected[0].packageName)
        assertEquals(1, connector.getPendingAppUsages().size)
        assertEquals("com.old.app", connector.getPendingAppUsages()[0].packageName)
    }

    @Test
    fun `clearPendingAppUsages empties the pending queue`() {
        connector.addAppUsage(packageName = "com.example.app", totalTimeInForeground = 1000L, lastTimeUsed = 2000L)
        assertEquals(1, connector.getPendingAppUsages().size)

        connector.clearPendingAppUsages()
        assertTrue(connector.getPendingAppUsages().isEmpty())
    }
}
