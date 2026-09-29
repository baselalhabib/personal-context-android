package com.baselalhabib.personalcontext.core.connectors

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallLogConnectorTest {

    private lateinit var connector: CallLogConnector

    @Before
    fun setUp() {
        connector = CallLogConnector()
    }

    @Test
    fun `id is call_log_connector`() {
        assertEquals("call_log_connector", connector.id)
    }

    @Test
    fun `addCallLog stages call log in pending queue`() {
        connector.addCallLog(number = "1234567890", duration = 45L, callType = "incoming", callerName = "John Doe")

        val pending = connector.getPendingCallLogs()
        assertEquals(1, pending.size)
        assertEquals("1234567890", pending[0].number)
        assertEquals(45L, pending[0].duration)
        assertEquals("incoming", pending[0].callType)
        assertEquals("John Doe", pending[0].callerName)
    }

    @Test
    fun `collect retrieves pending call logs and clears queue`() = runBlocking {
        connector.addCallLog(number = "111", duration = 10L, timestamp = 1000L)
        connector.addCallLog(number = "222", duration = 20L, timestamp = 2000L)

        val collected = connector.collect()

        assertEquals(2, collected.size)
        assertTrue(connector.getPendingCallLogs().isEmpty())
    }

    @Test
    fun `collect with sinceTimestamp filters out older pending call logs`() = runBlocking {
        connector.addCallLog(number = "111", duration = 10L, timestamp = 1000L)
        connector.addCallLog(number = "222", duration = 20L, timestamp = 2000L)

        val collected = connector.collect(sinceTimestamp = 1500L)

        assertEquals(1, collected.size)
        assertEquals("222", collected[0].number)
    }

    @Test
    fun `clearPendingCallLogs removes all staged call logs`() {
        connector.addCallLog(number = "111", duration = 10L)
        connector.clearPendingCallLogs()

        assertTrue(connector.getPendingCallLogs().isEmpty())
    }
}
