package com.baselalhabib.personalcontext.core.connectors

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteConnectorTest {

    private lateinit var connector: NoteConnector

    @Before
    fun setUp() {
        connector = NoteConnector()
    }

    @Test
    fun `id is note_connector and permissions are empty`() {
        assertEquals("note_connector", connector.id)
        assertTrue(connector.requiredPermissions.isEmpty())
    }

    @Test
    fun `addNote adds note to pending notes`() {
        val note = connector.addNote(title = "Test Title", content = "Test Content")

        assertEquals("Test Title", note.title)
        assertEquals("Test Content", note.content)
        assertEquals(1, connector.getPendingNotes().size)
    }

    @Test
    fun `collect returns all pending notes and clears queue when sinceTimestamp is null`() = runBlocking {
        connector.addNote(title = "Note 1", content = "Content 1")
        connector.addNote(title = "Note 2", content = "Content 2")

        val collected = connector.collect(sinceTimestamp = null)

        assertEquals(2, collected.size)
        assertTrue(connector.getPendingNotes().isEmpty())
    }

    @Test
    fun `collect filters by sinceTimestamp`() = runBlocking {
        connector.addNote(title = "Old Note", content = "Content", timestamp = 1000L)
        connector.addNote(title = "New Note", content = "Content", timestamp = 2000L)

        val collected = connector.collect(sinceTimestamp = 1500L)

        assertEquals(1, collected.size)
        assertEquals("New Note", collected[0].title)
        assertEquals(1, connector.getPendingNotes().size)
        assertEquals("Old Note", connector.getPendingNotes()[0].title)
    }
}
