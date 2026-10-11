package com.abridgefs.app.ai

/**
 * Builds runtime connectors from persisted profiles.
 * The default protocol set is deliberately small; callers can inject additional adapters
 * without changing the chat UI, registry, or profile persistence contract.
 */
class AIConnectorFactory(
    additionalAdapters: Map<AIProtocol, (AIProfile) -> AIConnector> = emptyMap()
) {
    private val adapters: Map<AIProtocol, (AIProfile) -> AIConnector> = mapOf(
        AIProtocol.OPENAI_COMPATIBLE to { profile -> OpenAICompatibleConnector(profile) },
        AIProtocol.ANTHROPIC to { profile -> AnthropicConnector(profile) },
        AIProtocol.GEMINI to { profile -> GeminiConnector(profile) }
    ) + additionalAdapters

    fun create(profile: AIProfile): AIConnector =
        adapters[profile.protocol]?.invoke(profile)
            ?: error("尚未注册 API 协议适配器：${profile.protocol}")

    fun connection(profile: AIProfile): AIConnection =
        AIConnection(id = profile.id, name = profile.name)
}
