package com.abridgefs.aps.development

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.abridgefs.aps.ai.AIConnector
import com.abridgefs.aps.ai.AIRequest
import com.abridgefs.aps.ai.AIResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentTaskExecutionServiceTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun clear() {
        context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE).edit()
            .remove("tasks").remove("tasks.corrupt_backup")
            .remove("events").remove("events.corrupt_backup").commit()
    }

    @Test
    fun successful_ai_handoff_saves_result_and_moves_task_to_review() = runBlocking {
        clear()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("exec-success", "ctx-exec", "Implement screen", description = "Add a settings panel"))
            store.transition("ctx-exec", "exec-success", DevelopmentTaskStatus.READY, 10L)
            var received: AIRequest? = null
            val connector = object : AIConnector {
                override suspend fun send(request: AIRequest): AIResponse {
                    received = request
                    return AIResponse("Implemented plan; no files were changed.")
                }
            }
            val result = DevelopmentTaskExecutionService().execute(
                "ctx-exec", "exec-success", store, connector
            ) { 20L }
            assertEquals(DevelopmentTaskStatus.REVIEW_REQUIRED, result.status)
            assertEquals("Implemented plan; no files were changed.", result.resultSummary)
            assertEquals(1, result.attempt)
            assertTrue(received?.userText?.contains("Implement screen") == true)
            assertTrue(received?.userText?.contains("Add a settings panel") == true)
            assertEquals(DevelopmentTaskStatus.REVIEW_REQUIRED, DevelopmentTaskStore(context).get("ctx-exec", "exec-success")?.status)
        } finally {
            clear()
        }
    }

    @Test
    fun ai_failure_marks_running_task_failed_without_exposing_exception_message() = runBlocking {
        clear()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("exec-fail", "ctx-exec", "Fail task"))
            store.transition("ctx-exec", "exec-fail", DevelopmentTaskStatus.READY, 10L)
            val connector = object : AIConnector {
                override suspend fun send(request: AIRequest): AIResponse {
                    error("sensitive provider detail")
                }
            }
            var failed = false
            try {
                DevelopmentTaskExecutionService().execute("ctx-exec", "exec-fail", store, connector) { 20L }
            } catch (_: IllegalStateException) {
                failed = true
            }
            assertTrue(failed)
            val saved = store.get("ctx-exec", "exec-fail")
            assertEquals(DevelopmentTaskStatus.FAILED, saved?.status)
            assertTrue(saved?.errorMessage?.contains("sensitive provider detail") == false)
            assertEquals(3, store.events("ctx-exec", "exec-fail").size)
        } finally {
            clear()
        }
    }
}
