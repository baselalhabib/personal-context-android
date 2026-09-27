package com.baselalhabib.personalcontext.core.connectors

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.baselalhabib.personalcontext.core.entities.LocationEntity
import com.baselalhabib.personalcontext.core.permissions.PermissionsHelper
import java.util.UUID

/**
 * Connector implementation for location context entities.
 * Supports both manual/staged locations and querying system location providers when [context] is supplied
 * and location permissions are granted.
 */
class LocationConnector(
    private val context: Context? = null
) : Connector<LocationEntity> {

    override val id: String = "location_connector"

    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val pendingLocations = mutableListOf<LocationEntity>()

    /**
     * Creates and stages a new [LocationEntity] into the connector's pending queue.
     */
    fun addLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        accuracy: Float? = null,
        provider: String? = null,
        id: String = UUID.randomUUID().toString(),
        timestamp: Long = System.currentTimeMillis(),
        source: String = "location"
    ): LocationEntity {
        val location = LocationEntity(
            id = id,
            timestamp = timestamp,
            source = source,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            accuracy = accuracy,
            provider = provider
        )
        synchronized(pendingLocations) {
            pendingLocations.add(location)
        }
        return location
    }

    /**
     * Stages an existing [LocationEntity] into the connector's pending queue.
     */
    fun addLocation(location: LocationEntity) {
        synchronized(pendingLocations) {
            pendingLocations.add(location)
        }
    }

    /**
     * Collects locations from both the pending queue and system location providers (if [context] is present
     * and location permissions are granted). Clears collected pending locations.
     */
    override suspend fun collect(sinceTimestamp: Long?): List<LocationEntity> {
        val results = mutableListOf<LocationEntity>()

        synchronized(pendingLocations) {
            val collectedPending = if (sinceTimestamp != null) {
                pendingLocations.filter { it.timestamp >= sinceTimestamp }
            } else {
                pendingLocations.toList()
            }
            results.addAll(collectedPending)
            pendingLocations.removeAll(collectedPending)
        }

        context?.let { ctx ->
            if (hasLocationPermission(ctx)) {
                results.addAll(querySystemLocation(ctx, sinceTimestamp))
            }
        }

        return results
    }

    private fun hasLocationPermission(ctx: Context): Boolean {
        return PermissionsHelper.isPermissionGranted(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ||
                PermissionsHelper.isPermissionGranted(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @Suppress("MissingPermission")
    private fun querySystemLocation(ctx: Context, sinceTimestamp: Long?): List<LocationEntity> {
        val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return emptyList()

        val results = mutableListOf<LocationEntity>()
        val providers = try {
            locationManager.getProviders(true)
        } catch (_: Exception) {
            emptyList()
        }

        for (providerName in providers) {
            try {
                val loc: Location? = locationManager.getLastKnownLocation(providerName)
                if (loc != null) {
                    val time = loc.time.takeIf { it > 0 } ?: System.currentTimeMillis()
                    if (sinceTimestamp == null || time >= sinceTimestamp) {
                        results.add(
                            LocationEntity(
                                id = "loc_${providerName}_${time}",
                                timestamp = time,
                                source = "location",
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                altitude = if (loc.hasAltitude()) loc.altitude else null,
                                accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
                                provider = providerName
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore providers that fail or throw SecurityException
            }
        }

        return results
    }

    /**
     * Returns a snapshot of all currently pending locations without clearing them.
     */
    fun getPendingLocations(): List<LocationEntity> {
        synchronized(pendingLocations) {
            return pendingLocations.toList()
        }
    }

    /**
     * Clears all pending locations.
     */
    fun clearPendingLocations() {
        synchronized(pendingLocations) {
            pendingLocations.clear()
        }
    }
}
