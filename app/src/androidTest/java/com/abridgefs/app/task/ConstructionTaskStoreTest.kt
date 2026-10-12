package com.owla19s.aps.task

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstructionTaskStoreTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun lifecycle_is_persisted_and_records_attempts_and_events() {
        val prefs = context.getSharedPreferences("aps_construction_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").remove("events").commit()
        try {
            val store = ConstructionTaskStore(context)
            store.create(ConstructionTask("task-1", "ctx-1", "Build feature", createdAt = 10L))
            store.markReady("ctx-1", "task-1", 20L)
            store.start("ctx-1", "task-1", 30L)
            store.requestReview("ctx-1", "task-1", 40L, "Implementation result")
            assertEquals(ConstructionTaskStatus.REVIEW_REQUIRED, ConstructionTaskStore(context).get("ctx-1", "task-1")?.status)
            store.succeedAfterReview("ctx-1", "task-1", 50L, "Review passed")
            val task = store.get("ctx-1", "task-1")!!
            assertEquals(ConstructionTaskStatus.SUCCEEDED, task.status)
            assertEquals(1, task.attempt)
            assertEquals(5, store.events("ctx-1", "task-1").size)
            assertEquals(0, store.all("another-context").size)
        } finally {
            prefs.edit().remove("tasks").remove("events").remove("tasks.corrupt_backup").remove("events.corrupt_backup").commit()
        }
    }

    @Test
    fun failed_task_can_be_retried_but_success_is_terminal() {
        val prefs = context.getSharedPreferences("aps_construction_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").remove("events").commit()
        try {
            val store = ConstructionTaskStore(context)
            store.create(ConstructionTask("task-2", "ctx-1", "Retryable task", createdAt = 1L))
            store.markReady("ctx-1", "task-2", 2L)
            store.start("ctx-1", "task-2", 3L)
            store.fail("ctx-1", "task-2", 4L, "network error")
            store.retry("ctx-1", "task-2", 5L)
            store.start("ctx-1", "task-2", 6L)
            assertEquals(2, store.get("ctx-1", "task-2")!!.attempt)
            store.requestReview("ctx-1", "task-2", 7L, "done")
            store.succeedAfterReview("ctx-1", "task-2", 8L, "passed")
            try {
                store.retry("ctx-1", "task-2", 9L)
                throw AssertionError("Expected terminal-state transition rejection")
            } catch (_: IllegalArgumentException) {
                assertTrue(store.get("ctx-1", "task-2")!!.status == ConstructionTaskStatus.SUCCEEDED)
            }
        } finally {
            prefs.edit().remove("tasks").remove("events").remove("tasks.corrupt_backup").remove("events.corrupt_backup").commit()
        }
    }
}
