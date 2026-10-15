package com.aps.ai

import kotlinx.coroutines.runBlocking
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderConnectorErrorRedactionTest {
    @Test
    fun gemini_model_list_error_redacts_api_key_values() = runBlocking {
        val error = modelListError(
            body = """{"key":"secret-gemini-json","x-goog-api-key":"secret-google-header","message":"invalid credentials"}""",
            connector = { profile, client -> GeminiConnector(profile, client) },
            baseUrl = "https://generativelanguage.googleapis.com/v1beta"
        )

        assertTrue(error is IOException)
        assertFalse(error?.message.orEmpty().contains("secret-gemini-json"))
        assertFalse(error?.message.orEmpty().contains("secret-google-header"))
        assertTrue(error?.message.orEmpty().contains("[redacted]"))
    }

    @Test
    fun anthropic_model_list_error_redacts_api_key_and_bearer_values() = runBlocking {
        val error = modelListError(
            body = """{"x-api-key":"secret-anthropic-header","authorization":"Bearer secret-bearer-token","message":"invalid credentials"}""",
            connector = { profile, client -> AnthropicConnector(profile, client) },
            baseUrl = "https://api.anthropic.com/v1"
        )

        assertTrue(error is IOException)
        assertFalse(error?.message.orEmpty().contains("secret-anthropic-header"))
        assertFalse(error?.message.orEmpty().contains("secret-bearer-token"))
        assertTrue(error?.message.orEmpty().contains("[redacted]"))
    }

    private suspend fun modelListError(
        body: String,
        connector: (AIProfile, OkHttpClient) -> AIModelCatalogConnector,
        baseUrl: String
    ): Throwable? {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val profile = AIProfile(
            name = "Test API",
            baseUrl = baseUrl,
            model = "model-a",
            apiKey = "secret-request-key"
        )

        return runCatching { connector(profile, client).listModels() }.exceptionOrNull()
    }
}
