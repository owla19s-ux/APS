package com.aps.ai

/**
 * Protocol descriptor. IDs are stable persisted identifiers; display names are UI-only.
 * New protocol adapters can register their own IDs without modifying this core type.
 */
data class AIProtocol(val id: String, val displayName: String) {
    init {
        require(id.isNotBlank()) { "API 协议 ID 不能为空" }
        require(displayName.isNotBlank()) { "API 协议名称不能为空" }
    }

    companion object {
        val OPENAI_COMPATIBLE = AIProtocol("openai-compatible", "OpenAI 兼容协议")
        val ANTHROPIC = AIProtocol("anthropic-messages", "Anthropic Messages")
        val GEMINI = AIProtocol("google-gemini", "Google Gemini")
        val BUILT_INS = listOf(OPENAI_COMPATIBLE, ANTHROPIC, GEMINI)

        fun fromStoredValue(value: String?): AIProtocol =
            BUILT_INS.firstOrNull {
                it.id.equals(value, ignoreCase = true) ||
                    (value.equals("OPENAI_COMPATIBLE", ignoreCase = true) && it == OPENAI_COMPATIBLE) ||
                    (value.equals("ANTHROPIC", ignoreCase = true) && it == ANTHROPIC) ||
                    (value.equals("GEMINI", ignoreCase = true) && it == GEMINI)
            } ?: value?.takeIf { it.isNotBlank() }?.let { AIProtocol(it, it) }
                ?: OPENAI_COMPATIBLE
    }
}

/** Optional capabilities exposed by connectors that can discover models and test chat. */
interface AIModelCatalogConnector : AIConnector {
    suspend fun listModels(): List<String>
    suspend fun testChatCompletion(): String
}
