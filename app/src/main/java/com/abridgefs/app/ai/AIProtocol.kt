package com.abridgefs.app.ai

/**
 * Wire protocol used by an AI profile. Provider identity is deliberately separate:
 * many providers expose an OpenAI-compatible endpoint, while one provider may expose
 * more than one API protocol.
 */
enum class AIProtocol(val displayName: String) {
    OPENAI_COMPATIBLE("OpenAI 兼容协议"),
    ANTHROPIC("Anthropic Messages"),
    GEMINI("Google Gemini");

    companion object {
        fun fromStoredValue(value: String?): AIProtocol =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: OPENAI_COMPATIBLE
    }
}

/** Optional capabilities exposed by connectors that can discover models and test chat. */
interface AIModelCatalogConnector : AIConnector {
    suspend fun listModels(): List<String>
    suspend fun testChatCompletion(): String
}
