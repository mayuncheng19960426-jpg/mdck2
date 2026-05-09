package com.sillyandroid.app.domain.repository

import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.data.entity.GenerationPresetEntity
import com.sillyandroid.app.data.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val db: AppDatabase) {

    private val presetDao = db.presetDao()
    private val apiConfigDao = db.apiConfigDao()

    // --- Presets ---
    fun getAllPresets(): Flow<List<GenerationPresetEntity>> = presetDao.getAllFlow()

    suspend fun getDefaultPreset(): GenerationPresetEntity? = presetDao.getDefault()

    suspend fun getPreset(id: Long): GenerationPresetEntity? = presetDao.getById(id)

    suspend fun insertPreset(preset: GenerationPresetEntity): Long = presetDao.insert(preset)

    suspend fun updatePreset(preset: GenerationPresetEntity) = presetDao.update(preset)

    suspend fun deletePreset(preset: GenerationPresetEntity) = presetDao.delete(preset)

    suspend fun setDefaultPreset(presetId: Long) {
        presetDao.clearOtherDefaults(presetId)
        val preset = presetDao.getById(presetId)
        if (preset != null) {
            presetDao.update(preset.copy(isDefault = true))
        }
    }

    // --- API Configs ---
    fun getAllApiConfigs(): Flow<List<ApiConfigEntity>> = apiConfigDao.getAllFlow()

    suspend fun getDefaultApiConfig(): ApiConfigEntity? = apiConfigDao.getDefault()

    suspend fun getApiConfig(id: Long): ApiConfigEntity? = apiConfigDao.getById(id)

    suspend fun insertApiConfig(config: ApiConfigEntity): Long = apiConfigDao.insert(config)

    suspend fun updateApiConfig(config: ApiConfigEntity) = apiConfigDao.update(config)

    suspend fun deleteApiConfig(config: ApiConfigEntity) = apiConfigDao.delete(config)

    suspend fun setDefaultApiConfig(configId: Long) {
        apiConfigDao.clearOtherDefaults(configId)
        val config = apiConfigDao.getById(configId)
        if (config != null) {
            apiConfigDao.update(config.copy(isDefault = true))
        }
    }
}
