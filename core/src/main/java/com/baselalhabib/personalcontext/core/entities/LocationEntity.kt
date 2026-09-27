package com.baselalhabib.personalcontext.core.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    override val id: String,
    override val timestamp: Long,
    override val source: String = "location",
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val provider: String? = null
) : ContextEntity
