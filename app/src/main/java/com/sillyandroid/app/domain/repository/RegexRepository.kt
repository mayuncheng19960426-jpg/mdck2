package com.sillyandroid.app.domain.repository

import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.data.entity.RegexRuleEntity
import kotlinx.coroutines.flow.Flow

class RegexRepository(private val db: AppDatabase) {

    private val dao = db.regexRuleDao()

    fun getEnabled(): Flow<List<RegexRuleEntity>> = dao.getEnabledFlow()

    fun getAll(): Flow<List<RegexRuleEntity>> = dao.getAllFlow()

    suspend fun getEnabledList(): List<RegexRuleEntity> = dao.getEnabled()

    suspend fun getAllList(): List<RegexRuleEntity> = dao.getAll()

    suspend fun getById(id: Long): RegexRuleEntity? = dao.getById(id)

    suspend fun insert(rule: RegexRuleEntity): Long = dao.insert(rule)

    suspend fun update(rule: RegexRuleEntity) = dao.update(rule)

    suspend fun delete(rule: RegexRuleEntity) = dao.delete(rule)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
