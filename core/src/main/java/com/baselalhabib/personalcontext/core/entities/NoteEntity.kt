package com.baselalhabib.personalcontext.core.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    override val id: String,
    override val timestamp: Long,
    override val source: String = "in_app",
    val title: String,
    val content: String
) : ContextEntity
