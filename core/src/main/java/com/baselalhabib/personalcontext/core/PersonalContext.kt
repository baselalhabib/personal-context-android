package com.baselalhabib.personalcontext.core

import android.content.Context
import com.baselalhabib.personalcontext.core.connectors.Connector
import com.baselalhabib.personalcontext.core.entities.AppUsageEntity
import com.baselalhabib.personalcontext.core.entities.CallLogEntity
import com.baselalhabib.personalcontext.core.entities.ContactEntity
import com.baselalhabib.personalcontext.core.entities.ContextEntity
import com.baselalhabib.personalcontext.core.entities.LocationEntity
import com.baselalhabib.personalcontext.core.entities.MessageEntity
import com.baselalhabib.personalcontext.core.entities.NoteEntity
import com.baselalhabib.personalcontext.core.query.ContextQuery
import com.baselalhabib.personalcontext.core.storage.PersonalContextDatabase
import kotlin.reflect.KClass

/**
 * Main entry point for the Personal Context library.
 * Provides APIs for querying stored context entities and syncing connectors with local storage.
 */
class PersonalContext(
    val database: PersonalContextDatabase
) {
    constructor(context: Context) : this(PersonalContextDatabase.getInstance(context))

    /**
     * Creates a new [ContextQuery] builder for the specified entity type [T].
     */
    inline fun <reified T : ContextEntity> query(): ContextQuery<T> {
        return query(T::class)
    }

    /**
     * Creates a new [ContextQuery] builder for the specified [entityClass].
     */
    fun <T : ContextEntity> query(entityClass: KClass<T>): ContextQuery<T> {
        return ContextQuery(database, entityClass)
    }

    /**
     * Collects raw context data from [connector] and stores it into the local database.
     * Returns the collected entities that were persisted.
     */
    suspend fun <T : ContextEntity> sync(
        connector: Connector<T>,
        sinceTimestamp: Long? = null
    ): List<T> {
        val collected = connector.collect(sinceTimestamp)
        if (collected.isNotEmpty()) {
            saveEntities(collected)
        }
        return collected
    }

    /**
     * Clears all context data from all database tables.
     */
    suspend fun clearAllData() {
        database.noteDao().deleteAll()
        database.messageDao().deleteAll()
        database.appUsageDao().deleteAll()
        database.locationDao().deleteAll()
        database.callLogDao().deleteAll()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun saveEntities(entities: List<ContextEntity>) {
        val first = entities.firstOrNull() ?: return
        when (first) {
            is NoteEntity -> database.noteDao().insertAll(entities as List<NoteEntity>)
            is MessageEntity -> database.messageDao().insertAll(entities as List<MessageEntity>)
            is AppUsageEntity -> database.appUsageDao().insertAll(entities as List<AppUsageEntity>)
            is LocationEntity -> database.locationDao().insertAll(entities as List<LocationEntity>)
            is CallLogEntity -> database.callLogDao().insertAll(entities as List<CallLogEntity>)
        }
    }
}
