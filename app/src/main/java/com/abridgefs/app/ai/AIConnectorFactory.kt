package com.owla19s.aps.ai

/**
 * Builds runtime connectors from persisted profiles.
 * The default protocol set is deliberately small; callers can inject additional adapters
 * without changing the chat UI, registry, or profile persistence contract.
 */
class AIConnectorFactory(
    additionalAdapters: Map<String, (AIProfile) -> AIConnector> = emptyMap()
) {
    private val adapters: Map<String, (AIProfile) -> AIConnector> = mapOf<String, (AIProfile) -> AIConnector>(
        AIProtocol.OPENAI_COMPATIBLE.id to { profile -> OpenAICompatibleConnector(profile) },
        AIProtocol.ANTHROPIC.id to { profile -> AnthropicConnector(profile) },
        AIProtocol.GEMINI.id to { profile -> GeminiConnector(profile) }
    ) + additionalAdapters

    fun create(profile: AIProfile): AIConnector =
        adapters[profile.protocol.id]?.invoke(profile)
            ?: error("尚未注册 API 协议适配器：${profile.protocol.id}")

    fun connection(profile: AIProfile): AIConnection =
        AIConnection(id = profile.id, name = profile.name)
}
