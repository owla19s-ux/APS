package com.aps.ai

/**
 * User-configured AI API profile. Provider identity and wire protocol are separate.
 * The API key is never included in toString() or UI summaries.
 */
data class AIProfile(
    val id: String = DEFAULT_ID,
    val name: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val protocol: AIProtocol = AIProtocol.OPENAI_COMPATIBLE
) {
    init {
        require(id.isNotBlank()) { "API Profile ID 不能为空" }
        require(name.isNotBlank()) { "API 名称不能为空" }
        require(baseUrl.isNotBlank()) { "API 地址不能为空" }
        require(model.isNotBlank()) { "模型名称不能为空" }
        require(apiKey.isNotBlank()) { "API Key 不能为空" }
    }

    val normalizedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/')

    override fun toString(): String =
        "AIProfile(id=$id, name=$name, baseUrl=${ApiErrorSanitizer.sanitize(baseUrl)}, model=$model, protocol=$protocol, apiKey=[redacted])"

    companion object {
        const val DEFAULT_ID = "default-ai"
    }
}
