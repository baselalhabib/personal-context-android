package com.baselalhabib.personalcontext.core.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    override val id: String,
    override val timestamp: Long,
    override val source: String = "sms",
    val sender: String,
    val body: String,
    val type: String = "inbox"
) : ContextEntity
