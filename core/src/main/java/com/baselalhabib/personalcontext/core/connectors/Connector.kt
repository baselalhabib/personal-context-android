package com.baselalhabib.personalcontext.core.connectors

import com.baselalhabib.personalcontext.core.entities.ContextEntity

/**
 * Abstract connector interface responsible for pulling raw Android data,
 * mapping it to a [ContextEntity], and making it available for local storage.
 */
interface Connector<T : ContextEntity> {
    /** Unique identifier for the connector (e.g. "sms_connector") */
    val id: String

    /** List of Android manifest permissions required to run this connector */
    val requiredPermissions: List<String>

    /**
     * Collects raw context data since [sinceTimestamp] (or all available history if null).
     */
    suspend fun collect(sinceTimestamp: Long? = null): List<T>
}
