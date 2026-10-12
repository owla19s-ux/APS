package com.owla19s.aps.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorSanitizerTest {
    @Test
    fun redactsProviderCredentialsAcrossJsonAndHeaderFormats() {
        val body = """{"apiKey":"secret-one","x-api-key":"secret-two","x-goog-api-key":"secret-three","authorization":"Bearer secret-four"}"""

        val sanitized = ApiErrorSanitizer.sanitize(body)

        assertTrue(sanitized.contains("[redacted]"))
        listOf("secret-one", "secret-two", "secret-three", "secret-four").forEach {
            assertFalse("Leaked credential: $it", sanitized.contains(it))
        }
    }

    @Test
    fun redactsUrlQueryParametersAndKeepsUsefulErrorText() {
        val body = """Permission denied for https://example.com/v1?token=secret-query&key=secret-key"""

        val sanitized = ApiErrorSanitizer.sanitize(body)

        assertTrue(sanitized.contains("Permission denied"))
        assertTrue(sanitized.contains("[URL with query redacted]"))
        assertFalse(sanitized.contains("secret-query"))
        assertFalse(sanitized.contains("secret-key"))
    }

    @Test
    fun redactsCredentialsEmbeddedInUrlUserInfo() {
        val body = "Request failed for https://user:secret-password@example.com/v1"

        val sanitized = ApiErrorSanitizer.sanitize(body)

        assertTrue(sanitized.contains("https://[credentials redacted]@example.com/v1"))
        assertFalse(sanitized.contains("secret-password"))
        assertFalse(sanitized.contains("user:secret-password"))
    }

    @Test
    fun blankProviderErrorUsesSafeFallback() {
        assertTrue(ApiErrorSanitizer.sanitize("  ").contains("服务未提供错误详情"))
    }
}
