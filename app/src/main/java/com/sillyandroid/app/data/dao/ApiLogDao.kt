package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.ApiLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiLogDao {

    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentFlow(): Flow<List<ApiLogEntity>>

    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<ApiLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ApiLogEntity): Long

    @Query("DELETE FROM api_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM api_logs")
    suspend fun clearAll()
}
