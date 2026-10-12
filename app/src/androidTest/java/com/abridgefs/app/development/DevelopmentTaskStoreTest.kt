package com.owla19s.aps.development

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DevelopmentTaskStoreTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun task_state_transitions_persist_and_failed_task_can_retry() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").remove("tasks.corrupt_backup").commit()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("task-1", "ctx-a", "Build screen", createdAtMillis = 10L))
            assertEquals(DevelopmentTaskStatus.CREATED, store.get("ctx-a", "task-1")?.status)
            store.transition("ctx-a", "task-1", DevelopmentTaskStatus.READY, 20L)
            val running = store.transition("ctx-a", "task-1", DevelopmentTaskStatus.RUNNING, 30L)
            assertEquals(1, running.attempt)
            val failed = store.transition(
                "ctx-a", "task-1", DevelopmentTaskStatus.FAILED, 40L, errorMessage = "test failed"
            )
            assertEquals("test failed", failed.errorMessage)
            store.transition("ctx-a", "task-1", DevelopmentTaskStatus.READY, 50L)
            val retry = store.transition("ctx-a", "task-1", DevelopmentTaskStatus.RUNNING, 60L)
            assertEquals(2, retry.attempt)
            val done = store.transition(
                "ctx-a", "task-1", DevelopmentTaskStatus.COMPLETED, 70L, resultSummary = "tests passed"
            )
            assertEquals("tests passed", done.resultSummary)
            assertEquals(DevelopmentTaskStatus.COMPLETED, DevelopmentTaskStore(context).get("ctx-a", "task-1")?.status)
        } finally {
            prefs.edit().remove("tasks").remove("tasks.corrupt_backup").commit()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun completed_task_cannot_be_reopened() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").commit()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("task-terminal", "ctx-a", "Terminal test"))
            store.transition("ctx-a", "task-terminal", DevelopmentTaskStatus.READY, 1L)
            store.transition("ctx-a", "task-terminal", DevelopmentTaskStatus.CANCELLED, 2L)
            store.transition("ctx-a", "task-terminal", DevelopmentTaskStatus.READY, 3L)
        } finally {
            prefs.edit().remove("tasks").commit()
        }
    }

    @Test
    fun task_lookup_is_context_scoped_and_unknown_task_is_absent() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").commit()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("task-a", "ctx-a", "Task A"))
            assertNull(store.get("ctx-b", "task-a"))
            assertEquals(1, store.all("ctx-a").size)
            assertEquals(0, store.all("ctx-b").size)
        } finally {
            prefs.edit().remove("tasks").commit()
        }
    }
}
