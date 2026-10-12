package com.owla19s.aps.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiEndpointTest {
    @Test
    fun appendsEndpointToExistingBasePath() {
        assertEquals(
            "https://api.example.com/custom/v1/chat/completions",
            ApiEndpoint.build("https://api.example.com/custom/v1/", "chat/completions").toString()
        )
    }

    @Test
    fun addsAnthropicV1PrefixOnlyWhenMissing() {
        assertEquals(
            "https://api.example.com/custom/v1/messages",
            ApiEndpoint.build("https://api.example.com/custom", "messages", ensureV1 = true).toString()
        )
        assertEquals(
            "https://api.example.com/custom/v1/messages",
            ApiEndpoint.build("https://api.example.com/custom/v1", "messages", ensureV1 = true).toString()
        )
    }

    @Test
    fun preservesBaseUrlQueryParametersAfterAppendingEndpointPath() {
        assertEquals(
            "https://api.example.com/v1/models?tenant=example",
            ApiEndpoint.build("https://api.example.com/v1?tenant=example", "models").toString()
        )
    }

    @Test
    fun removesTrailingSlashBeforeQueryParameters() {
        assertEquals(
            "https://api.example.com/v1/models?tenant=example",
            ApiEndpoint.build("https://api.example.com/v1/?tenant=example", "models").toString()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBaseUrlWithFragment() {
        ApiEndpoint.build("https://api.example.com/v1#fragment", "models")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBaseUrlWithEmbeddedCredentials() {
        ApiEndpoint.build("https://user:secret@api.example.com/v1", "models")
    }

    @Test(expected = IllegalArgumentException::class)
    fun baseUrlValidationRejectsEmbeddedCredentialsBeforePersistence() {
        ApiEndpoint.validateBaseUrl("https://user:secret@api.example.com/v1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPlainHttpBaseUrlToProtectApiKeys() {
        ApiEndpoint.validateBaseUrl("http://api.example.com/v1")
    }

    @Test
    fun geminiStreamingQueryIsAddedAfterPathConstruction() {
        val endpoint = ApiEndpoint.build(
            "https://generativelanguage.googleapis.com/v1beta",
            "models/gemini-test:streamGenerateContent"
        ).newBuilder().addQueryParameter("alt", "sse").build()

        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:streamGenerateContent?alt=sse",
            endpoint.toString()
        )
    }
}
