package com.sillyandroid.app.domain

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.sillyandroid.app.data.entity.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 角色卡导入导出工具。
 *
 * 支持 SillyTavern character card v2 JSON 格式的导入和导出。
 */
object CharacterImporter {

    private val gson = Gson()

    /**
     * 从 URI 导入角色卡（JSON 格式）。
     */
    suspend fun importFromJson(context: Context, uri: Uri): CharacterEntity? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val json = reader.readText()
            reader.close()

            parseCharacterCard(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 JSON 字符串解析角色卡。
     * 兼容 SillyTavern v2 格式：顶层的 data 字段，或 spec 字段。
     */
    fun parseCharacterCard(json: String): CharacterEntity? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject

            // Try SillyTavern v2 format: { "data": { ... } }
            val data = if (root.has("data")) root.getAsJsonObject("data") else root

            CharacterEntity(
                name = data.get("name")?.asString ?: "",
                description = data.get("description")?.asString ?: "",
                personality = data.get("personality")?.asString ?: "",
                firstMessage = data.get("first_mes")?.asString ?: data.get("first_message")?.asString ?: "",
                scenario = data.get("scenario")?.asString ?: "",
                exampleDialogue = data.get("mes_example")?.asString ?: data.get("example_dialogue")?.asString ?: "",
                systemPrompt = data.get("system_prompt")?.asString ?: "",
                postHistoryInstructions = data.get("post_history_instructions")?.asString ?: "",
                creatorNotes = data.get("creator_notes")?.asString ?: "",
                importFormat = "json"
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将角色卡导出为 JSON 字符串（兼容 SillyTavern v2 格式）。
     */
    fun exportToJson(character: CharacterEntity): String {
        val dataMap = mapOf(
            "name" to character.name,
            "description" to character.description,
            "personality" to character.personality,
            "first_mes" to character.firstMessage,
            "scenario" to character.scenario,
            "mes_example" to character.exampleDialogue,
            "system_prompt" to character.systemPrompt,
            "post_history_instructions" to character.postHistoryInstructions,
            "creator_notes" to character.creatorNotes,
            "character_version" to "2"
        )
        return gson.toJson(mapOf("spec" to "chara_card_v2", "data" to dataMap))
    }
}
