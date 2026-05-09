package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.RegexRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexRuleDao {

    @Query("SELECT * FROM regex_rules WHERE enabled = 1 ORDER BY sort_order ASC")
    fun getEnabledFlow(): Flow<List<RegexRuleEntity>>

    @Query("SELECT * FROM regex_rules ORDER BY sort_order ASC")
    fun getAllFlow(): Flow<List<RegexRuleEntity>>

    @Query("SELECT * FROM regex_rules WHERE enabled = 1 ORDER BY sort_order ASC")
    suspend fun getEnabled(): List<RegexRuleEntity>

    @Query("SELECT * FROM regex_rules ORDER BY sort_order ASC")
    suspend fun getAll(): List<RegexRuleEntity>

    @Query("SELECT * FROM regex_rules WHERE id = :id")
    suspend fun getById(id: Long): RegexRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RegexRuleEntity): Long

    @Update
    suspend fun update(entity: RegexRuleEntity)

    @Delete
    suspend fun delete(entity: RegexRuleEntity)

    @Query("DELETE FROM regex_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
