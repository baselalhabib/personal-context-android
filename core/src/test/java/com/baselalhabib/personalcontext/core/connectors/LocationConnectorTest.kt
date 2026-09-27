package com.baselalhabib.personalcontext.core.connectors

import com.baselalhabib.personalcontext.core.entities.LocationEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationConnectorTest {

    private lateinit var connector: LocationConnector

    @Before
    fun setUp() {
        connector = LocationConnector()
    }

    @Test
    fun `id is location_connector`() {
        assertEquals("location_connector", connector.id)
    }

    @Test
    fun `addLocation stages location in pending queue`() {
        connector.addLocation(latitude = 37.7749, longitude = -122.4194)

        val pending = connector.getPendingLocations()
        assertEquals(1, pending.size)
        assertEquals(37.7749, pending[0].latitude, 0.0001)
        assertEquals(-122.4194, pending[0].longitude, 0.0001)
    }

    @Test
    fun `collect retrieves pending locations and clears queue`() = runBlocking {
        connector.addLocation(latitude = 37.7749, longitude = -122.4194, timestamp = 1000L)
        connector.addLocation(latitude = 40.7128, longitude = -74.0060, timestamp = 2000L)

        val collected = connector.collect()

        assertEquals(2, collected.size)
        assertTrue(connector.getPendingLocations().isEmpty())
    }

    @Test
    fun `collect with sinceTimestamp filters out older pending locations`() = runBlocking {
        connector.addLocation(latitude = 10.0, longitude = 20.0, timestamp = 1000L)
        connector.addLocation(latitude = 30.0, longitude = 40.0, timestamp = 2000L)

        val collected = connector.collect(sinceTimestamp = 1500L)

        assertEquals(1, collected.size)
        assertEquals(30.0, collected[0].latitude, 0.0001)
    }

    @Test
    fun `clearPendingLocations removes all staged locations`() {
        connector.addLocation(latitude = 10.0, longitude = 20.0)
        connector.clearPendingLocations()

        assertTrue(connector.getPendingLocations().isEmpty())
    }
}
