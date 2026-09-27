package com.baselalhabib.personalcontext.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.baselalhabib.personalcontext.core.entities.AppUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appUsages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage ORDER BY lastTimeUsed DESC")
    fun getAllAppUsages(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY lastTimeUsed DESC")
    fun getAppUsagesBetween(startTime: Long, endTime: Long): Flow<List<AppUsageEntity>>

    @Query("DELETE FROM app_usage")
    suspend fun deleteAll()
}
