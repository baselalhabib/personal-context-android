package com.baselalhabib.personalcontext.core

import androidx.room.InvalidationTracker
import com.baselalhabib.personalcontext.core.connectors.AppUsageConnector
import com.baselalhabib.personalcontext.core.connectors.LocationConnector
import com.baselalhabib.personalcontext.core.connectors.MessageConnector
import com.baselalhabib.personalcontext.core.connectors.NoteConnector
import com.baselalhabib.personalcontext.core.entities.AppUsageEntity
import com.baselalhabib.personalcontext.core.entities.LocationEntity
import com.baselalhabib.personalcontext.core.entities.MessageEntity
import com.baselalhabib.personalcontext.core.entities.NoteEntity
import com.baselalhabib.personalcontext.core.storage.AppUsageDao
import com.baselalhabib.personalcontext.core.storage.LocationDao
import com.baselalhabib.personalcontext.core.storage.MessageDao
import com.baselalhabib.personalcontext.core.storage.NoteDao
import com.baselalhabib.personalcontext.core.storage.PersonalContextDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersonalContextTest {

    private lateinit var fakeDatabase: PersonalContextDatabase
    private lateinit var personalContext: PersonalContext

    @Before
    fun setUp() {
        fakeDatabase = FakePersonalContextDatabase()
        personalContext = PersonalContext(fakeDatabase)
    }

    @Test
    fun `sync inserts collected notes into database and query retrieves them`() = runBlocking {
        val connector = NoteConnector()
        connector.addNote(title = "Note 1", content = "Content 1", timestamp = 1000L)
        connector.addNote(title = "Note 2", content = "Content 2", timestamp = 2000L)

        val synced = personalContext.sync(connector)

        assertEquals(2, synced.size)

        val retrieved = personalContext.query<NoteEntity>().execute()

        assertEquals(2, retrieved.size)
        assertEquals("Note 2", retrieved[0].title)
        assertEquals("Note 1", retrieved[1].title)
    }

    @Test
    fun `query between filters by startTime and endTime`() = runBlocking {
        val noteConnector = NoteConnector()
        noteConnector.addNote(title = "Old Note", content = "Content", timestamp = 1000L)
        noteConnector.addNote(title = "Mid Note", content = "Content", timestamp = 2000L)
        noteConnector.addNote(title = "New Note", content = "Content", timestamp = 3000L)

        personalContext.sync(noteConnector)

        val filtered = personalContext.query<NoteEntity>()
            .between(1500L, 2500L)
            .execute()

        assertEquals(1, filtered.size)
        assertEquals("Mid Note", filtered[0].title)
    }

    @Test
    fun `query since and until work as expected`() = runBlocking {
        val noteConnector = NoteConnector()
        noteConnector.addNote(title = "Note 100", content = "Content", timestamp = 100L)
        noteConnector.addNote(title = "Note 200", content = "Content", timestamp = 200L)
        noteConnector.addNote(title = "Note 300", content = "Content", timestamp = 300L)

        personalContext.sync(noteConnector)

        val since200 = personalContext.query<NoteEntity>()
            .since(200L)
            .execute()

        assertEquals(2, since200.size)

        val until200 = personalContext.query<NoteEntity>()
            .until(200L)
            .execute()

        assertEquals(2, until200.size)
    }

    @Test
    fun `query fromSource filters by source string`() = runBlocking {
        val noteConnector = NoteConnector()
        noteConnector.addNote(title = "In App Note", content = "C1", source = "in_app")
        noteConnector.addNote(title = "Synced Note", content = "C2", source = "sync_service")

        personalContext.sync(noteConnector)

        val filtered = personalContext.query<NoteEntity>()
            .fromSource("sync_service")
            .execute()

        assertEquals(1, filtered.size)
        assertEquals("Synced Note", filtered[0].title)
    }

    @Test
    fun `query limit restricts maximum results`() = runBlocking {
        val noteConnector = NoteConnector()
        noteConnector.addNote(title = "N1", content = "C", timestamp = 100L)
        noteConnector.addNote(title = "N2", content = "C", timestamp = 200L)
        noteConnector.addNote(title = "N3", content = "C", timestamp = 300L)

        personalContext.sync(noteConnector)

        val limited = personalContext.query<NoteEntity>()
            .limit(2)
            .execute()

        assertEquals(2, limited.size)
    }

    @Test
    fun `sync and query work for MessageEntity`() = runBlocking {
        val messageConnector = MessageConnector()
        messageConnector.addMessage(sender = "+1234567890", body = "Hello World", timestamp = 1000L)

        personalContext.sync(messageConnector)

        val messages = personalContext.query<MessageEntity>().execute()

        assertEquals(1, messages.size)
        assertEquals("+1234567890", messages[0].sender)
        assertEquals("Hello World", messages[0].body)
    }

    @Test
    fun `sync and query work for AppUsageEntity`() = runBlocking {
        val usageConnector = AppUsageConnector()
        usageConnector.addAppUsage(
            packageName = "com.example.app",
            totalTimeInForeground = 5000L,
            lastTimeUsed = 1000L
        )

        personalContext.sync(usageConnector)

        val usages = personalContext.query<AppUsageEntity>().execute()

        assertEquals(1, usages.size)
        assertEquals("com.example.app", usages[0].packageName)
    }

    @Test
    fun `sync and query work for LocationEntity`() = runBlocking {
        val locationConnector = LocationConnector()
        locationConnector.addLocation(
            latitude = 37.7749,
            longitude = -122.4194,
            timestamp = 1000L
        )

        personalContext.sync(locationConnector)

        val locations = personalContext.query<LocationEntity>().execute()

        assertEquals(1, locations.size)
        assertEquals(37.7749, locations[0].latitude, 0.0001)
        assertEquals(-122.4194, locations[0].longitude, 0.0001)
    }

    @Test
    fun `clearAllData removes all stored entities from database`() = runBlocking {
        val noteConnector = NoteConnector()
        noteConnector.addNote(title = "Note", content = "Content")
        personalContext.sync(noteConnector)

        val messageConnector = MessageConnector()
        messageConnector.addMessage(sender = "Sender", body = "Body")
        personalContext.sync(messageConnector)

        val locationConnector = LocationConnector()
        locationConnector.addLocation(latitude = 12.34, longitude = 56.78)
        personalContext.sync(locationConnector)

        personalContext.clearAllData()

        assertTrue(personalContext.query<NoteEntity>().execute().isEmpty())
        assertTrue(personalContext.query<MessageEntity>().execute().isEmpty())
        assertTrue(personalContext.query<LocationEntity>().execute().isEmpty())
    }

    private class FakePersonalContextDatabase : PersonalContextDatabase() {
        private val noteDaoFake = FakeNoteDao()
        private val messageDaoFake = FakeMessageDao()
        private val appUsageDaoFake = FakeAppUsageDao()
        private val locationDaoFake = FakeLocationDao()

        override fun noteDao(): NoteDao = noteDaoFake
        override fun messageDao(): MessageDao = messageDaoFake
        override fun appUsageDao(): AppUsageDao = appUsageDaoFake
        override fun locationDao(): LocationDao = locationDaoFake

        override fun clearAllTables() {}
        override fun createInvalidationTracker(): InvalidationTracker {
            return InvalidationTracker(this, "notes", "messages", "app_usage", "locations")
        }
    }

    private class FakeNoteDao : NoteDao {
        private val notes = mutableListOf<NoteEntity>()
        private val flow = MutableStateFlow<List<NoteEntity>>(emptyList())

        override suspend fun insertAll(notes: List<NoteEntity>) {
            this.notes.addAll(notes)
            flow.value = this.notes.sortedByDescending { it.timestamp }
        }

        override fun getAllNotes(): Flow<List<NoteEntity>> = flow

        override fun getNotesBetween(startTime: Long, endTime: Long): Flow<List<NoteEntity>> {
            return flow.map { list -> list.filter { it.timestamp in startTime..endTime } }
        }

        override suspend fun deleteAll() {
            notes.clear()
            flow.value = emptyList()
        }
    }

    private class FakeMessageDao : MessageDao {
        private val messages = mutableListOf<MessageEntity>()
        private val flow = MutableStateFlow<List<MessageEntity>>(emptyList())

        override suspend fun insertAll(messages: List<MessageEntity>) {
            this.messages.addAll(messages)
            flow.value = this.messages.sortedByDescending { it.timestamp }
        }

        override fun getAllMessages(): Flow<List<MessageEntity>> = flow

        override fun getMessagesBetween(startTime: Long, endTime: Long): Flow<List<MessageEntity>> {
            return flow.map { list -> list.filter { it.timestamp in startTime..endTime } }
        }

        override suspend fun deleteAll() {
            messages.clear()
            flow.value = emptyList()
        }
    }

    private class FakeAppUsageDao : AppUsageDao {
        private val usages = mutableListOf<AppUsageEntity>()
        private val flow = MutableStateFlow<List<AppUsageEntity>>(emptyList())

        override suspend fun insertAll(appUsages: List<AppUsageEntity>) {
            this.usages.addAll(appUsages)
            flow.value = this.usages.sortedByDescending { it.lastTimeUsed }
        }

        override fun getAllAppUsages(): Flow<List<AppUsageEntity>> = flow

        override fun getAppUsagesBetween(startTime: Long, endTime: Long): Flow<List<AppUsageEntity>> {
            return flow.map { list -> list.filter { it.timestamp in startTime..endTime } }
        }

        override suspend fun deleteAll() {
            usages.clear()
            flow.value = emptyList()
        }
    }

    private class FakeLocationDao : LocationDao {
        private val locations = mutableListOf<LocationEntity>()
        private val flow = MutableStateFlow<List<LocationEntity>>(emptyList())

        override suspend fun insertAll(locations: List<LocationEntity>) {
            this.locations.addAll(locations)
            flow.value = this.locations.sortedByDescending { it.timestamp }
        }

        override fun getAllLocations(): Flow<List<LocationEntity>> = flow

        override fun getLocationsBetween(startTime: Long, endTime: Long): Flow<List<LocationEntity>> {
            return flow.map { list -> list.filter { it.timestamp in startTime..endTime } }
        }

        override suspend fun deleteAll() {
            locations.clear()
            flow.value = emptyList()
        }
    }
}
