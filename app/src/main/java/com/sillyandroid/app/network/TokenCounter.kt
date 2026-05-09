package com.sillyandroid.app.network

/**
 * 简易 Token 计数器。
 *
 * 使用字符级近似进行 Token 估算（1 token ≈ 4 chars for English, 1.5 for Chinese）。
 * APK 环境不引入完整的 tiktoken 库，用近似算法替代。
 */
object TokenCounter {

    // Approximate: 1 token ≈ 4 characters for English
    private const val CHARS_PER_TOKEN_EN = 4.0

    // Approximate: 1 token ≈ 1.5 characters for CJK
    private const val CHARS_PER_TOKEN_CJK = 1.5

    fun count(text: String): Int {
        if (text.isEmpty()) return 0

        var cjkChars = 0
        var otherChars = 0

        for (ch in text) {
            if (isCJK(ch)) cjkChars++ else otherChars++
        }

        return ((cjkChars / CHARS_PER_TOKEN_CJK) + (otherChars / CHARS_PER_TOKEN_EN))
            .toInt()
            .coerceAtLeast(1)
    }

    fun countMessages(messages: List<Map<String, String>>): Int {
        var total = 0
        for (msg in messages) {
            total += count(msg["content"] ?: "")
            total += count(msg["role"] ?: "")
            total += 4 // overhead per message
        }
        return total + 2 // priming overhead
    }

    private fun isCJK(ch: Char): Boolean {
        return ch.code in 0x4E00..0x9FFF || // CJK Unified Ideographs
                ch.code in 0x3400..0x4DBF || // CJK Unified Ideographs Extension A
                ch.code in 0x20000..0x2A6DF || // CJK Unified Ideographs Extension B
                ch.code in 0x3040..0x309F || // Hiragana
                ch.code in 0x30A0..0x30FF || // Katakana
                ch.code in 0xAC00..0xD7AF    // Hangul Syllables
    }
}
