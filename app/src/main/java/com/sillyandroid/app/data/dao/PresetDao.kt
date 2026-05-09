package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.GenerationPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {

    @Query("SELECT * FROM generation_presets ORDER BY name ASC")
    fun getAllFlow(): Flow<List<GenerationPresetEntity>>

    @Query("SELECT * FROM generation_presets ORDER BY name ASC")
    suspend fun getAll(): List<GenerationPresetEntity>

    @Query("SELECT * FROM generation_presets WHERE id = :id")
    suspend fun getById(id: Long): GenerationPresetEntity?

    @Query("SELECT * FROM generation_presets WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): GenerationPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GenerationPresetEntity): Long

    @Update
    suspend fun update(entity: GenerationPresetEntity)

    @Delete
    suspend fun delete(entity: GenerationPresetEntity)

    @Query("DELETE FROM generation_presets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE generation_presets SET is_default = 0 WHERE is_default = 1 AND id != :exceptId")
    suspend fun clearOtherDefaults(exceptId: Long)
}
