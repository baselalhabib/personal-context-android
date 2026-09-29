package com.baselalhabib.personalcontext.core.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    override val id: String,
    override val timestamp: Long,
    override val source: String = "contacts",
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val lookupKey: String? = null
) : ContextEntity
