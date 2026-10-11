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
}
