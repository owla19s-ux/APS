package com.aps.ai

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiProtocolConnectorTest {
    @Test
    fun anthropic_adapter_uses_messages_contract_and_api_key_header() = runBlocking {
        var captured: Request? = null
        var body = ""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            body = Buffer().also { buffer -> chain.request().body?.writeTo(buffer) }.readUtf8()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("""{"content":[{"type":"text","text":"Anthropic reply"}]}"""
                    .toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val profile = AIProfile(
            name = "Anthropic test", baseUrl = "https://api.anthropic.com/v1",
            model = "claude-test", apiKey = "anthropic-secret", protocol = AIProtocol.ANTHROPIC
        )

        val response = AnthropicConnector(profile, client).send(AIRequest(null, "hello"))

        assertEquals("Anthropic reply", response.text)
        assertEquals("https://api.anthropic.com/v1/messages", captured?.url.toString())
        assertEquals("anthropic-secret", captured?.header("x-api-key"))
        assertEquals("2023-06-01", captured?.header("anthropic-version"))
        assertTrue(body.contains("\"max_tokens\":2048"))
        assertTrue(body.contains("\"role\":\"user\""))
    }

    @Test
    fun gemini_adapter_uses_generate_content_contract() = runBlocking {
        var captured: Request? = null
        var body = ""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            body = Buffer().also { buffer -> chain.request().body?.writeTo(buffer) }.readUtf8()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("""{"candidates":[{"content":{"parts":[{"text":"Gemini reply"}]}}]}"""
                    .toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val profile = AIProfile(
            name = "Gemini test",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            model = "gemini-test", apiKey = "gemini-secret", protocol = AIProtocol.GEMINI
        )

        val response = GeminiConnector(profile, client).send(AIRequest(null, "hello"))

        assertEquals("Gemini reply", response.text)
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent",
            captured?.url.toString()
        )
        assertEquals("gemini-secret", captured?.header("x-goog-api-key"))
        assertTrue(body.contains("\"role\":\"user\""))
        assertTrue(body.contains("\"parts\""))
    }

    @Test
    fun factory_routes_profiles_by_stable_protocol_id_and_accepts_additional_adapters() {
        val customProtocolId = "test-custom-protocol"
        val customAdapter = object : AIConnector {
            override suspend fun send(request: AIRequest): AIResponse = AIResponse("custom")
        }
        val factory = AIConnectorFactory(mapOf(customProtocolId to { customAdapter }))
        val profile = AIProfile(
            name = "Custom test", baseUrl = "https://example.com",
            model = "test-model", apiKey = "secret",
            protocol = AIProtocol(customProtocolId, "Custom protocol")
        )

        assertTrue(factory.create(profile) === customAdapter)
        assertTrue(factory.create(profile.copy(protocol = AIProtocol.ANTHROPIC)) is AnthropicConnector)
        assertTrue(factory.create(profile.copy(protocol = AIProtocol.GEMINI)) is GeminiConnector)
    }

    @Test
    fun anthropic_model_catalog_uses_v1_models_endpoint() = runBlocking {
        var captured: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("""{"data":[{"id":"claude-test"}]}"""
                    .toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val profile = AIProfile(
            name = "Anthropic models", baseUrl = "https://api.anthropic.com/v1",
            model = "claude-test", apiKey = "secret", protocol = AIProtocol.ANTHROPIC
        )

        assertEquals(listOf("claude-test"), AnthropicConnector(profile, client).listModels())
        assertEquals("https://api.anthropic.com/v1/models", captured?.url.toString())
    }

    @Test
    fun gemini_model_catalog_filters_models_without_generate_content() = runBlocking {
        var captured: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("""{"models":[{"name":"models/gemini-flash","supportedGenerationMethods":["generateContent"]},{"name":"models/embed-only","supportedGenerationMethods":["embedContent"]}]}"""
                    .toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val profile = AIProfile(
            name = "Gemini models",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            model = "gemini-flash", apiKey = "secret", protocol = AIProtocol.GEMINI
        )

        assertEquals(listOf("gemini-flash"), GeminiConnector(profile, client).listModels())
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models",
            captured?.url.toString()
        )
    }

}
