package com.baselalhabib.personalcontext.core.entities

/**
 * Base interface for any personal context entity stored in the database.
 */
interface ContextEntity {
    val id: String
    val timestamp: Long
    val source: String
}
