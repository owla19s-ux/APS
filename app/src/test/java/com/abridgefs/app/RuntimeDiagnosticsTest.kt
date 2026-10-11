package com.owla19s.aps

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeDiagnosticsTest {
    @Test
    fun apiFailureCategoriesDistinguishConfigurationFromNetworkErrors() {
        assertEquals("invalid_configuration", RuntimeDiagnostics.classifyApiFailure(IllegalArgumentException("bad URL")))
        assertEquals("http_error", RuntimeDiagnostics.classifyApiFailure(IOException("HTTP 404"), "404"))
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
