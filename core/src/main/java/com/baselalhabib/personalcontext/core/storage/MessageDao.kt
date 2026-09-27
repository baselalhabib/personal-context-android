package com.baselalhabib.personalcontext.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.baselalhabib.personalcontext.core.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getMessagesBetween(startTime: Long, endTime: Long): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}
