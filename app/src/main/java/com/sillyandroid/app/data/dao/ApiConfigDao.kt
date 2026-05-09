package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiConfigDao {

    @Query("SELECT * FROM api_configs ORDER BY name ASC")
    fun getAllFlow(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_configs ORDER BY name ASC")
    suspend fun getAll(): List<ApiConfigEntity>

    @Query("SELECT * FROM api_configs WHERE id = :id")
    suspend fun getById(id: Long): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): ApiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ApiConfigEntity): Long

    @Update
    suspend fun update(entity: ApiConfigEntity)

    @Delete
    suspend fun delete(entity: ApiConfigEntity)

    @Query("DELETE FROM api_configs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE api_configs SET is_default = 0 WHERE is_default = 1 AND id != :exceptId")
    suspend fun clearOtherDefaults(exceptId: Long)
}
