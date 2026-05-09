package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.CharacterScriptRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterScriptRuleDao {

    @Query("SELECT * FROM character_script_rules WHERE character_id = :characterId AND enabled = 1 ORDER BY priority ASC")
    suspend fun getEnabled(characterId: Long): List<CharacterScriptRuleEntity>

    @Query("SELECT * FROM character_script_rules WHERE character_id = :characterId ORDER BY priority ASC")
    fun getByCharacterFlow(characterId: Long): Flow<List<CharacterScriptRuleEntity>>

    @Query("SELECT * FROM character_script_rules WHERE character_id = :characterId ORDER BY priority ASC")
    suspend fun getByCharacter(characterId: Long): List<CharacterScriptRuleEntity>

    @Query("SELECT * FROM character_script_rules WHERE id = :id")
    suspend fun getById(id: Long): CharacterScriptRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CharacterScriptRuleEntity): Long

    @Update
    suspend fun update(entity: CharacterScriptRuleEntity)

    @Delete
    suspend fun delete(entity: CharacterScriptRuleEntity)

    @Query("DELETE FROM character_script_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
