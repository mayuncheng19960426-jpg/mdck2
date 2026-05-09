package com.sillyandroid.app.cardbuilder

/**
 * 解析 LLM 返回的结构化 XML 标签块。
 */
object CardBuilderParser {

    data class ParsedBlock(
        val tag: String,        // e.g. "main-character-origin"
        val name: String,       // e.g. "艾琳"
        val content: String,    // tag 内的完整内容
        val templateId: Int,    // 对应的模板 ID
        val targetEntity: String // "character" | "worldbook" | "mvu" | "regex"
    )

    // 匹配所有 <tag-name>...</tag-name> 格式的块
    private val blockRegex = Regex(
        """<\s*([a-zA-Z0-9_-]+?)\s*-\s*([^>]+?)\s*>\s*(.*?)\s*<\s*/\s*\1\s*-\s*\2\s*>""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    // 匹配无后缀标签 <tag>...</tag>
    private val simpleBlockRegex = Regex(
        """<\s*([a-zA-Z0-9_-]+?)\s*>\s*(.*?)\s*<\s*/\s*\1\s*>""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    /**
     * 解析 LLM 返回的完整文本，提取所有标签块。
     */
    fun parse(text: String): List<ParsedBlock> {
        val blocks = mutableListOf<ParsedBlock>()

        // First try tag-name format
        for (match in blockRegex.findAll(text)) {
            val tag = match.groupValues[1].trim()
            val name = match.groupValues[2].trim()
            val content = match.groupValues[3].trim()

            val template = CardBuilderTemplates.templates.find { it.tag == tag }
            blocks.add(
                ParsedBlock(
                    tag = tag,
                    name = name,
                    content = content,
                    templateId = template?.id ?: 0,
                    targetEntity = template?.targetEntity ?: "character"
                )
            )
        }

        // Then try simple format for tags like <output-format>, <variable-summary>, etc.
        for (match in simpleBlockRegex.findAll(text)) {
            val tag = match.groupValues[1].trim()
            val content = match.groupValues[2].trim()

            // Skip if already captured by tag-name regex
            if (blocks.any { it.tag == tag }) continue

            val template = CardBuilderTemplates.templates.find { it.tag == tag }
            blocks.add(
                ParsedBlock(
                    tag = tag,
                    name = tag,
                    content = content,
                    templateId = template?.id ?: 0,
                    targetEntity = template?.targetEntity ?: "character"
                )
            )
        }

        return blocks
    }

    /**
     * 把解析出的块按模板 ID 分组。
     */
    fun groupByTemplate(blocks: List<ParsedBlock>): Map<Int, List<ParsedBlock>> {
        return blocks.groupBy { it.templateId }
    }
}
