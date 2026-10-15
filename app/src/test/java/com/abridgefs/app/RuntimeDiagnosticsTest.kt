package com.aps

import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeDiagnosticsTest {
    @Test
    fun logRotationThresholdPreservesFilesUntilTheConfiguredLimit() {
        assertEquals(false, RuntimeDiagnostics.shouldRotateLog(0L))
        assertEquals(false, RuntimeDiagnostics.shouldRotateLog(1_048_575L))
        assertEquals(true, RuntimeDiagnostics.shouldRotateLog(1_048_576L))
        assertEquals(true, RuntimeDiagnostics.shouldRotateLog(1_048_577L))
    }

    @Test
    fun rotatedLogFileUsesUniqueNameWhenTimestampCollides() {
        val directory = createTempDir(prefix = "aps-log-rotation-")
        try {
            File(directory, "aps-2026-10-15-12345.log").writeText("previous segment")
            File(directory, "aps-2026-10-15-12345-1.log").writeText("another segment")

            val candidate = RuntimeDiagnostics.nextRotatedLogFile(directory, "2026-10-15", 12345L)

            assertEquals("aps-2026-10-15-12345-2.log", candidate.name)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun apiFailureCategoriesDistinguishConfigurationFromNetworkErrors() {
        assertEquals("invalid_configuration", RuntimeDiagnostics.classifyApiFailure(IllegalArgumentException("bad URL")))
        assertEquals("authentication", RuntimeDiagnostics.classifyApiFailure(IOException("HTTP 401"), "401"))
        assertEquals("permission_denied", RuntimeDiagnostics.classifyApiFailure(IOException("HTTP 403"), "403"))
        assertEquals("endpoint_not_found", RuntimeDiagnostics.classifyApiFailure(IOException("HTTP 404"), "404"))
        assertEquals("rate_limited", RuntimeDiagnostics.classifyApiFailure(IOException("HTTP 429"), "429"))
        assertEquals("provider_server_error", RuntimeDiagnostics.classifyApiFailure(IOException("HTTP 503"), "503"))
        assertEquals("http_error", RuntimeDiagnostics.classifyApiFailure(IOException("HTTP 418"), "418"))
        assertEquals("timeout", RuntimeDiagnostics.classifyApiFailure(SocketTimeoutException("timeout")))
        assertEquals("dns_or_host", RuntimeDiagnostics.classifyApiFailure(UnknownHostException("host")))
        assertEquals("network_io", RuntimeDiagnostics.classifyApiFailure(IOException("connection reset")))
        assertNull(RuntimeDiagnostics.classifyApiFailure(null))
    }

    @Test
    fun loggingBeforeApplicationInitializationDoesNotBreakCallers() {
        RuntimeDiagnostics.record("app.test", "started")
        RuntimeDiagnostics.recordApiEvent(
            event = "api.test",
            outcome = "failed",
            durationMs = 12,
            error = IOException("HTTP 401: provider returned an error")
        )
        RuntimeDiagnostics.recordCrash(
            Thread.currentThread(),
            IllegalStateException("exception details must not be copied into crash logs")
        )
    }

    @Test
    fun crashFormattingRedactsJsonSecretsAndUrlQueryParameters() {
        val error = IllegalStateException(
            """Request failed: {"apiKey":"secret-one","x-goog-api-key":"secret-two"} https://example.com/v1?token=secret-three"""
        )
        val formatted = RuntimeDiagnostics.formatCrash(Thread.currentThread(), error)

        org.junit.Assert.assertTrue(formatted.contains("\"apiKey\":\"[redacted]"))
        org.junit.Assert.assertTrue(formatted.contains("\"x-goog-api-key\":\"[redacted]"))
        org.junit.Assert.assertTrue(formatted.contains("[URL with query redacted]"))
        org.junit.Assert.assertFalse(formatted.contains("secret-one"))
        org.junit.Assert.assertFalse(formatted.contains("secret-two"))
        org.junit.Assert.assertFalse(formatted.contains("secret-three"))
    }

    @Test
    fun crashFormattingIncludesStackTraceCauseAndRedactsSensitiveValues() {
        val cause = IllegalArgumentException("api_key=secret-cause")
        val error = IllegalStateException("token=secret-token", cause)
        val formatted = RuntimeDiagnostics.formatCrash(Thread.currentThread(), error)

        org.junit.Assert.assertTrue(formatted.contains("event=app.crash outcome=uncaught"))
        org.junit.Assert.assertTrue(formatted.contains("error_type=java.lang.IllegalStateException"))
        org.junit.Assert.assertTrue(formatted.contains(" exception_message=token=[redacted]"))
        org.junit.Assert.assertTrue(formatted.contains(" caused_by_type=java.lang.IllegalArgumentException"))
        org.junit.Assert.assertTrue(formatted.contains("message=api_key=[redacted]"))
        org.junit.Assert.assertTrue(formatted.contains("\n  at "))
        org.junit.Assert.assertFalse(formatted.contains("secret-token"))
        org.junit.Assert.assertFalse(formatted.contains("secret-cause"))
    }
}
