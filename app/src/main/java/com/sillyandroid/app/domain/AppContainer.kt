package com.sillyandroid.app.domain

import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.domain.repository.*
import com.sillyandroid.app.network.ChatApiClient

/**
 * 手动依赖注入容器。
 * 由 Application 创建，所有 ViewModel 通过此容器获取依赖。
 */
class AppContainer(private val db: AppDatabase) {

    // --- Repositories ---
    val characterRepository = CharacterRepository(db)
    val chatRepository = ChatRepository(db)
    val worldBookRepository = WorldBookRepository(db)
    val regexRepository = RegexRepository(db)
    val settingsRepository = SettingsRepository(db)
    val agentRepository = AgentRepository(db)

    // --- Network (lazy: depends on settings) ---
    val chatApiClient = ChatApiClient(settingsRepository)
}
