package com.sillyandroid.app.engine

import com.sillyandroid.app.data.entity.RegexRuleEntity

/**
 * 正则引擎。
 *
 * 实现 SillyTavern 的 Regex 扩展功能：
 * - 预处理：在发送给 LLM 之前对 prompt 进行替换
 * - 后处理：在接收 LLM 响应之后对内容进行替换
 */
class RegexEngine {

    /**
     * 应用正则规则列表。
     *
     * @param text 输入文本
     * @param rules 正则规则列表
     * @return 处理后的文本
     */
    fun applyRules(text: String, rules: List<RegexRuleEntity>): String {
        var result = text
        for (rule in rules) {
            if (!rule.enabled) continue
            if (rule.findRegex.isBlank()) continue

            try {
                val regex = Regex(rule.findRegex, setOf(RegexOption.MULTILINE))
                result = regex.replace(result, rule.replaceString)
            } catch (e: Exception) {
                // Skip invalid regex patterns
                continue
            }
        }
        return result
    }

    /**
     * 预处理：在发送给 LLM 前处理 prompt。
     */
    fun preProcess(prompt: String, rules: List<RegexRuleEntity>): String {
        return applyRules(prompt, rules.filter { it.source != "post" })
    }

    /**
     * 后处理：处理 LLM 响应。
     */
    fun postProcess(response: String, rules: List<RegexRuleEntity>): String {
        return applyRules(response, rules.filter { it.source != "pre" || it.runOnEdit })
    }
}
