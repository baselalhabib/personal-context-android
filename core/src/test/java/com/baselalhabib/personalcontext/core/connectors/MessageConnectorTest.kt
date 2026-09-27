package com.baselalhabib.personalcontext.core.connectors

import android.Manifest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MessageConnectorTest {

    private lateinit var connector: MessageConnector

    @Before
    fun setUp() {
        connector = MessageConnector()
    }

    @Test
    fun `id is message_connector and permissions contain READ_SMS`() {
        assertEquals("message_connector", connector.id)
        assertEquals(1, connector.requiredPermissions.size)
        assertEquals(Manifest.permission.READ_SMS, connector.requiredPermissions[0])
    }

    @Test
    fun `addMessage adds message to pending messages`() {
        val message = connector.addMessage(sender = "+123456789", body = "Hello test")

        assertEquals("+123456789", message.sender)
        assertEquals("Hello test", message.body)
        assertEquals("inbox", message.type)
        assertEquals("sms", message.source)
        assertEquals(1, connector.getPendingMessages().size)
    }

    @Test
    fun `collect returns all pending messages and clears queue when sinceTimestamp is null`() = runBlocking {
        connector.addMessage(sender = "Sender 1", body = "Body 1")
        connector.addMessage(sender = "Sender 2", body = "Body 2")

        val collected = connector.collect(sinceTimestamp = null)

        assertEquals(2, collected.size)
        assertTrue(connector.getPendingMessages().isEmpty())
    }

    @Test
    fun `collect filters by sinceTimestamp`() = runBlocking {
        connector.addMessage(sender = "Old Sender", body = "Old Body", timestamp = 1000L)
        connector.addMessage(sender = "New Sender", body = "New Body", timestamp = 2000L)

        val collected = connector.collect(sinceTimestamp = 1500L)

        assertEquals(1, collected.size)
        assertEquals("New Sender", collected[0].sender)
        assertEquals(1, connector.getPendingMessages().size)
        assertEquals("Old Sender", connector.getPendingMessages()[0].sender)
    }

    @Test
    fun `clearPendingMessages empties the pending queue`() {
        connector.addMessage(sender = "Sender", body = "Body")
        assertEquals(1, connector.getPendingMessages().size)

        connector.clearPendingMessages()
        assertTrue(connector.getPendingMessages().isEmpty())
    }
}
