package com.sillyandroid.app.engine

import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.data.entity.ChatVariableEntity
import com.sillyandroid.app.data.entity.CharacterScriptRuleEntity

/**
 * MVU 引擎。
 *
 * 实现变量系统 + 角色脚本规则的完整管线：
 *   1. 条件求值   → 检查规则 condition 是否满足
 *   2. 动作执行   → 执行 rule.action 修改变量
 *   3. 模板替换   → {{varName}} → 变量当前值
 *
 * 支持的动作语法：
 *   set name value     — 设置变量（自动推断类型）
 *   add name value     — 数值加
 *   sub name value     — 数值减
 *   inc name           — 整数自增 1
 *   dec name           — 整数自减 1
 *   toggle name        — 布尔翻转
 *   push name value    — 追加到逗号分隔的字符串列表
 */
class MvuEngine(private val db: AppDatabase) {

    private val variableDao get() = db.chatVariableDao()
    private val ruleDao get() = db.characterScriptRuleDao()

    // ==================== 模板替换 ====================

    /**
     * 将文本中的 {{varName}} 替换为变量当前值。
     * 未知变量替换为 "(未设置)"。
     */
    suspend fun expandVariables(text: String, chatId: Long): String {
        val variables = variableDao.getByChat(chatId).associateBy { it.name }
        val regex = Regex("\\{\\{\\s*(\\w+)\\s*\\}\\}")

        return regex.replace(text) { match ->
            val varName = match.groupValues[1]
            variables[varName]?.displayValue ?: "(未设置)"
        }
    }

    // ==================== 规则执行 ====================

    /**
     * 对指定角色和阶段执行所有启用的脚本规则。
     *
     * @return 受影响的变量名列表（用于 UI 更新）
     */
    suspend fun executeRules(
        characterId: Long,
        chatId: Long,
        phase: String,         // "before_prompt" | "after_response"
        messageCount: Int = 0  // 当前消息编号
    ): List<String> {
        val rules = ruleDao.getEnabled(characterId).filter { it.triggerPhase == phase }
        if (rules.isEmpty()) return emptyList()

        val variables = variableDao.getByChat(chatId).associateBy { it.name }.toMutableMap()
        val affected = mutableListOf<String>()

        for (rule in rules) {
            if (!evaluateCondition(rule.condition, variables, messageCount)) continue

            val changedVars = executeAction(rule.action, chatId, variables)
            affected.addAll(changedVars)
        }

        return affected.distinct()
    }

    /**
     * 直接执行单个动作（暴露给 ChatPipeline 使用）。
     */
    suspend fun executeAction(action: String, chatId: Long) {
        val variables = variableDao.getByChat(chatId).associateBy { it.name }.toMutableMap()
        executeAction(action, chatId, variables)
    }

    /**
     * 手动设置变量值（从 UI 或调试面板）。
     */
    suspend fun setManualVariable(chatId: Long, name: String, value: String) {
        val entity = valueToEntity(chatId, name, value)
        variableDao.upsert(entity)
    }

    /**
     * 获取聊天所有变量的当前快照。
     */
    suspend fun getVariables(chatId: Long): Map<String, String> {
        return variableDao.getByChat(chatId).associate { it.name to it.displayValue }
    }

    // ==================== 条件求值 ====================

    private fun evaluateCondition(
        condition: String?,
        variables: Map<String, ChatVariableEntity>,
        messageCount: Int
    ): Boolean {
        if (condition.isNullOrBlank() || condition == "always") return true

        // Split: "varName >= value" or "varName operator value"
        val parts = condition.trim().split("\\s+".toRegex(), limit = 3)
        if (parts.size < 3) return false

        val varName = parts[0]
        val operator = parts[1]
        val operand = parts[2]

        // Handle special "round" variable
        if (varName == "round" || varName == "message_count") {
            return compareNumeric(messageCount.toFloat(), operator, operand)
        }

        val variable = variables[varName] ?: return false

        return when (variable.type) {
            "int", "float" -> {
                val currentVal = if (variable.type == "int") variable.intValue.toFloat() else variable.floatValue
                compareNumeric(currentVal, operator, operand)
            }
            "boolean" -> {
                val boolVal = variable.booleanValue
                val targetVal = operand.lowercase() == "true"
                when (operator) {
                    "==" -> boolVal == targetVal
                    "!=" -> boolVal != targetVal
                    else -> false
                }
            }
            else -> {
                // String comparison
                when (operator) {
                    "==" -> variable.stringValue == operand
                    "!=" -> variable.stringValue != operand
                    "contains" -> variable.stringValue.contains(operand, ignoreCase = true)
                    "startsWith" -> variable.stringValue.startsWith(operand)
                    "endsWith" -> variable.stringValue.endsWith(operand)
                    else -> false
                }
            }
        }
    }

    private fun compareNumeric(current: Float, operator: String, operand: String): Boolean {
        val target = operand.toFloatOrNull() ?: return false
        return when (operator) {
            ">" -> current > target
            "<" -> current < target
            ">=" -> current >= target
            "<=" -> current <= target
            "==" -> current == target
            "!=" -> current != target
            else -> false
        }
    }

    // ==================== 动作执行 ====================

    private fun executeAction(
        action: String,
        chatId: Long,
        variables: MutableMap<String, ChatVariableEntity>
    ): List<String> {
        val parts = action.trim().split("\\s+".toRegex(), limit = 3)
        if (parts.isEmpty()) return emptyList()

        val command = parts[0].lowercase()
        val varName = parts.getOrNull(1) ?: return emptyList()
        val operand = parts.getOrNull(2) ?: ""

        val current = variables[varName]
        val changedVars = mutableListOf(varName)

        when (command) {
            "set" -> {
                val entity = valueToEntity(chatId, varName, operand)
                variables[varName] = entity
                variableDao.upsert(entity)
            }
            "add" -> {
                val entity = current ?: ChatVariableEntity(chatId = chatId, name = varName, type = "int")
                if (entity.type in listOf("int", "float")) {
                    val newValue = (entity.intValue + (operand.toIntOrNull() ?: 0)).coerceAtLeast(0)
                    val updated = entity.copy(intValue = newValue, type = "int")
                    variables[varName] = updated
                    variableDao.upsert(updated)
                }
            }
            "sub" -> {
                val entity = current ?: ChatVariableEntity(chatId = chatId, name = varName, type = "int")
                if (entity.type in listOf("int", "float")) {
                    val newValue = (entity.intValue - (operand.toIntOrNull() ?: 0)).coerceAtLeast(0)
                    val updated = entity.copy(intValue = newValue, type = "int")
                    variables[varName] = updated
                    variableDao.upsert(updated)
                }
            }
            "inc", "increment" -> {
                val entity = current ?: ChatVariableEntity(chatId = chatId, name = varName, type = "int")
                val updated = entity.copy(intValue = entity.intValue + 1, type = "int")
                variables[varName] = updated
                variableDao.upsert(updated)
            }
            "dec", "decrement" -> {
                val entity = current ?: ChatVariableEntity(chatId = chatId, name = varName, type = "int")
                val updated = entity.copy(intValue = (entity.intValue - 1).coerceAtLeast(0), type = "int")
                variables[varName] = updated
                variableDao.upsert(updated)
            }
            "toggle" -> {
                val entity = current ?: ChatVariableEntity(chatId = chatId, name = varName, type = "boolean")
                val updated = entity.copy(booleanValue = !entity.booleanValue, type = "boolean")
                variables[varName] = updated
                variableDao.upsert(updated)
            }
            "push" -> {
                val entity = current ?: ChatVariableEntity(chatId = chatId, name = varName, type = "string")
                val currentList = entity.stringValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val newList = if (operand.isNotBlank() && !currentList.contains(operand)) {
                    currentList + operand
                } else currentList
                val updated = entity.copy(stringValue = newList.joinToString(", "), type = "string")
                variables[varName] = updated
                variableDao.upsert(updated)
            }
        }

        return changedVars
    }

    /** 将用户输入的值字符串解析为正确的类型实体 */
    private fun valueToEntity(chatId: Long, name: String, value: String): ChatVariableEntity {
        val trimmed = value.trim()
        return when {
            trimmed.lowercase() == "true" || trimmed.lowercase() == "false" ->
                ChatVariableEntity(chatId = chatId, name = name, type = "boolean", booleanValue = trimmed.toBoolean())
            trimmed.toIntOrNull() != null ->
                ChatVariableEntity(chatId = chatId, name = name, type = "int", intValue = trimmed.toInt())
            trimmed.toFloatOrNull() != null ->
                ChatVariableEntity(chatId = chatId, name = name, type = "float", floatValue = trimmed.toFloat())
            else ->
                ChatVariableEntity(chatId = chatId, name = name, type = "string", stringValue = trimmed)
        }
    }
}
