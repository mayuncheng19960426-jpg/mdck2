package com.sillyandroid.app.cardbuilder

import com.sillyandroid.app.data.entity.*
import com.sillyandroid.app.domain.AppContainer

/**
 * 将搓卡解析结果采纳到对应实体。
 */
class CardBuilderAdapter(private val container: AppContainer) {

    suspend fun adopt(block: CardBuilderParser.ParsedBlock, characterId: Long) {
        when (block.targetEntity) {
            "character" -> adoptToCharacter(block, characterId)
            "worldbook" -> adoptToWorldBook(block)
            "mvu" -> adoptToMvu(block, characterId)
            "regex" -> adoptToRegex(block)
        }
    }

    private suspend fun adoptToCharacter(block: CardBuilderParser.ParsedBlock, characterId: Long) {
        val character = container.characterRepository.getById(characterId) ?: return
        when (block.tag) {
            "main-character-origin" ->
                container.characterRepository.update(character.copy(personality = mergeField(character.personality, block.content, "## 角色原点\n")))
            "main-character-profile" ->
                container.characterRepository.update(character.copy(description = mergeField(character.description, block.content, "## 角色画像\n")))
            "aesthetic-guideline" ->
                container.characterRepository.update(character.copy(postHistoryInstructions = mergeField(character.postHistoryInstructions, block.content, "## 美学纲领\n")))
            "output-format" ->
                container.characterRepository.update(character.copy(systemPrompt = mergeField(character.systemPrompt, block.content, "## 输出格式\n")))
        }
    }

    private suspend fun adoptToWorldBook(block: CardBuilderParser.ParsedBlock) {
        val hint = CardBuilderTemplates.getById(block.templateId)?.worldBookHint
        val bookCount = container.database.worldBookDao().countBooks()
        val worldBookId = if (bookCount > 0) 1L else {
            container.worldBookRepository.insertBook(WorldBookEntity(name = "搓卡生成", description = "自动创建", isDefault = false))
        }

        container.worldBookRepository.insertEntry(
            WorldBookEntryEntity(
                worldBookId = worldBookId,
                key = block.name,
                content = block.content,
                comment = "搓卡生成: ${block.tag}",
                priority = hint?.priority ?: 10,
                position = hint?.position ?: "before_char",
                depth = hint?.depth ?: 0,
                isSelective = hint?.selective ?: true,
                order = 0
            )
        )
    }

    private suspend fun adoptToMvu(block: CardBuilderParser.ParsedBlock, characterId: Long) {
        if (block.tag != "mvu-design") return
        val rules = parseMvuRules(block.content)
        for (rule in rules) {
            container.database.characterScriptRuleDao().insert(
                CharacterScriptRuleEntity(
                    characterId = characterId, scriptName = rule.name,
                    triggerPhase = rule.phase, condition = rule.condition.ifBlank { null },
                    action = rule.action, priority = rule.priority
                )
            )
        }
    }

    private suspend fun adoptToRegex(block: CardBuilderParser.ParsedBlock) {
        when (block.tag) {
            "status-bar-regex-rules" -> {
                // Parse individual regex rules from the status-bar-regex-rules block
                val regexRules = parseRegexRules(block.content)
                for (rule in regexRules) {
                    container.database.regexRuleDao().insert(
                        RegexRuleEntity(
                            scriptName = rule.name,
                            findRegex = rule.findRegex,
                            replaceString = rule.replaceString,
                            isGlobal = false,
                            enabled = rule.enabled
                        )
                    )
                }
            }
            else -> {
                container.database.regexRuleDao().insert(
                    RegexRuleEntity(
                        scriptName = "搓卡: ${block.tag}",
                        findRegex = block.content,
                        replaceString = "",
                        isGlobal = false,
                        enabled = false
                    )
                )
            }
        }
    }

    // ==================== helpers ====================

    data class MvuRuleParsed(val name: String, val phase: String, val condition: String, val action: String, val priority: Int)

    private fun parseMvuRules(content: String): List<MvuRuleParsed> {
        val rules = mutableListOf<MvuRuleParsed>()
        var name = ""; var phase = "before_prompt"; var condition = ""; var action = ""; var priority = 0

        for (line in content.lines()) {
            val t = line.trim()
            when {
                t.startsWith("名称:") || t.startsWith("规则名:") -> {
                    name = t.substringAfter(":").trim(); phase = "before_prompt"; condition = ""; action = ""; priority = 0
                }
                t.startsWith("触发阶段:") -> phase = if ("after" in t) "after_response" else "before_prompt"
                t.startsWith("条件:") -> condition = t.substringAfter(":").trim()
                t.startsWith("动作:") -> {
                    action = t.substringAfter(":").trim()
                    if (name.isNotBlank() && action.isNotBlank()) rules.add(MvuRuleParsed(name, phase, condition, action, priority))
                }
                t.startsWith("优先级:") -> priority = t.substringAfter(":").trim().toIntOrNull() ?: 0
            }
        }
        return rules
    }

    data class RegexRuleParsed(
        val name: String,
        val findRegex: String,
        val replaceString: String,
        val enabled: Boolean
    )

    private fun parseRegexRules(content: String): List<RegexRuleParsed> {
        val rules = mutableListOf<RegexRuleParsed>()
        var name = ""
        var findRegex = ""
        var replaceString = ""
        var enabled = true

        for (line in content.lines()) {
            val t = line.trim()
            when {
                t.startsWith("规则名:") -> {
                    name = t.substringAfter(":").trim()
                    findRegex = ""; replaceString = ""; enabled = true
                }
                t.startsWith("查找正则:") -> findRegex = t.substringAfter(":").trim()
                t.startsWith("替换为:") -> {
                    replaceString = t.substringAfter(":").trim()
                    if (name.isNotBlank() && findRegex.isNotBlank()) {
                        rules.add(RegexRuleParsed(name, findRegex, replaceString, enabled))
                    }
                }
                t.startsWith("启用:") -> enabled = t.substringAfter(":").trim().lowercase() == "true"
            }
        }
        return rules
    }

    private fun mergeField(existing: String, newContent: String, header: String): String {
        return if (existing.contains(header)) {
            existing.substringBefore(header) + header + newContent + "\n"
        } else "$existing\n\n$header$newContent"
    }
}
