package com.baselalhabib.personalcontext.core.query

import com.baselalhabib.personalcontext.core.entities.AppUsageEntity
import com.baselalhabib.personalcontext.core.entities.ContextEntity
import com.baselalhabib.personalcontext.core.entities.LocationEntity
import com.baselalhabib.personalcontext.core.entities.MessageEntity
import com.baselalhabib.personalcontext.core.entities.NoteEntity
import com.baselalhabib.personalcontext.core.storage.PersonalContextDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

/**
 * Fluent query builder for retrieving [ContextEntity] records from [PersonalContextDatabase].
 */
class ContextQuery<T : ContextEntity>(
    private val database: PersonalContextDatabase,
    private val entityClass: KClass<T>
) {
    private var startTime: Long? = null
    private var endTime: Long? = null
    private var sourceFilter: String? = null
    private var limit: Int? = null

    /**
     * Filters entities with timestamp between [startTime] and [endTime] (inclusive).
     */
    fun between(startTime: Long, endTime: Long): ContextQuery<T> = apply {
        require(startTime <= endTime) { "startTime ($startTime) must be <= endTime ($endTime)" }
        this.startTime = startTime
        this.endTime = endTime
    }

    /**
     * Filters entities with timestamp >= [startTime].
     */
    fun since(startTime: Long): ContextQuery<T> = apply {
        this.startTime = startTime
    }

    /**
     * Filters entities with timestamp <= [endTime].
     */
    fun until(endTime: Long): ContextQuery<T> = apply {
        this.endTime = endTime
    }

    /**
     * Filters entities by source matching [source].
     */
    fun fromSource(source: String): ContextQuery<T> = apply {
        this.sourceFilter = source
    }

    /**
     * Limits the maximum number of entities returned to [count].
     */
    fun limit(count: Int): ContextQuery<T> = apply {
        require(count > 0) { "Limit must be greater than 0" }
        this.limit = count
    }

    /**
     * Returns a reactive [Flow] emitting the matching entities whenever the database changes.
     */
    @Suppress("UNCHECKED_CAST")
    fun asFlow(): Flow<List<T>> {
        val rawFlow: Flow<List<ContextEntity>> = when (entityClass) {
            NoteEntity::class -> queryNotes()
            MessageEntity::class -> queryMessages()
            AppUsageEntity::class -> queryAppUsage()
            LocationEntity::class -> queryLocations()
            else -> throw IllegalArgumentException("Unsupported entity class: ${entityClass.qualifiedName}")
        }

        return rawFlow.map { list ->
            var filtered = list
            val start = startTime
            val end = endTime

            if (start != null && end == null) {
                filtered = filtered.filter { it.timestamp >= start }
            } else if (start == null && end != null) {
                filtered = filtered.filter { it.timestamp <= end }
            }

            sourceFilter?.let { src ->
                filtered = filtered.filter { it.source == src }
            }

            limit?.let { lim ->
                filtered = filtered.take(lim)
            }

            filtered as List<T>
        }
    }

    /**
     * Executes the query once asynchronously and returns the matching entities.
     */
    suspend fun execute(): List<T> {
        return asFlow().first()
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryNotes(): Flow<List<ContextEntity>> {
        val start = startTime
        val end = endTime
        val dao = database.noteDao()
        return if (start != null && end != null) {
            dao.getNotesBetween(start, end)
        } else {
            dao.getAllNotes()
        } as Flow<List<ContextEntity>>
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryMessages(): Flow<List<ContextEntity>> {
        val start = startTime
        val end = endTime
        val dao = database.messageDao()
        return if (start != null && end != null) {
            dao.getMessagesBetween(start, end)
        } else {
            dao.getAllMessages()
        } as Flow<List<ContextEntity>>
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryAppUsage(): Flow<List<ContextEntity>> {
        val start = startTime
        val end = endTime
        val dao = database.appUsageDao()
        return if (start != null && end != null) {
            dao.getAppUsagesBetween(start, end)
        } else {
            dao.getAllAppUsages()
        } as Flow<List<ContextEntity>>
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryLocations(): Flow<List<ContextEntity>> {
        val start = startTime
        val end = endTime
        val dao = database.locationDao()
        return if (start != null && end != null) {
            dao.getLocationsBetween(start, end)
        } else {
            dao.getAllLocations()
        } as Flow<List<ContextEntity>>
    }
}
