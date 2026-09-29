package com.baselalhabib.personalcontext.core.connectors

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactConnectorTest {

    private lateinit var connector: ContactConnector

    @Before
    fun setUp() {
        connector = ContactConnector()
    }

    @Test
    fun `id is contact_connector`() {
        assertEquals("contact_connector", connector.id)
    }

    @Test
    fun `addContact stages contact in pending queue`() {
        connector.addContact(name = "Jane Doe", phoneNumber = "555-0199", email = "jane@example.com")

        val pending = connector.getPendingContacts()
        assertEquals(1, pending.size)
        assertEquals("Jane Doe", pending[0].name)
        assertEquals("555-0199", pending[0].phoneNumber)
        assertEquals("jane@example.com", pending[0].email)
    }

    @Test
    fun `collect retrieves pending contacts and clears queue`() = runBlocking {
        connector.addContact(name = "Alice", timestamp = 1000L)
        connector.addContact(name = "Bob", timestamp = 2000L)

        val collected = connector.collect()

        assertEquals(2, collected.size)
        assertTrue(connector.getPendingContacts().isEmpty())
    }

    @Test
    fun `collect with sinceTimestamp filters out older pending contacts`() = runBlocking {
        connector.addContact(name = "Alice", timestamp = 1000L)
        connector.addContact(name = "Bob", timestamp = 2000L)

        val collected = connector.collect(sinceTimestamp = 1500L)

        assertEquals(1, collected.size)
        assertEquals("Bob", collected[0].name)
    }

    @Test
    fun `clearPendingContacts removes all staged contacts`() {
        connector.addContact(name = "Alice")
        connector.clearPendingContacts()

        assertTrue(connector.getPendingContacts().isEmpty())
    }
}
