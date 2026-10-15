package com.owla19s.aps

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeDiagnosticsPersistenceInstrumentedTest {
    @Test
    fun recordedEventsCanBeReadBackFromAppPrivateStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        RuntimeDiagnostics.initialize(context)

        val event = "diagnostics.persistence.instrumented_test"
        RuntimeDiagnostics.record(event, "success", durationMs = 7)

        val logs = RuntimeDiagnostics.readRecentLogs(context)
        assertTrue("Expected the recorded event in the local diagnostics file", logs.contains("event=$event"))
        assertTrue("Expected the outcome in the local diagnostics file", logs.contains("outcome=success"))
        assertTrue("Expected the duration in the local diagnostics file", logs.contains("duration_ms=7"))
    }
}
