package com.baselalhabib.personalcontext.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.baselalhabib.personalcontext.core.entities.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getContactsBetween(startTime: Long, endTime: Long): Flow<List<ContactEntity>>

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
