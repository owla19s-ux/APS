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
        prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
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
            val review = store.transition(
                "ctx-a", "task-1", DevelopmentTaskStatus.REVIEW_REQUIRED, 70L, resultSummary = "implementation ready"
            )
            assertEquals("implementation ready", review.resultSummary)
            val done = store.transition(
                "ctx-a", "task-1", DevelopmentTaskStatus.COMPLETED, 80L, resultSummary = "review passed"
            )
            assertEquals("review passed", done.resultSummary)
            assertEquals(DevelopmentTaskStatus.COMPLETED, DevelopmentTaskStore(context).get("ctx-a", "task-1")?.status)
            val events = DevelopmentTaskStore(context).events("ctx-a", "task-1")
            assertEquals(8, events.size)
            assertEquals(null, events.first().fromStatus)
            assertEquals(DevelopmentTaskStatus.CREATED, events.first().toStatus)
            assertEquals(DevelopmentTaskStatus.READY, events[4].toStatus)
            assertEquals("失败后重新排队", events[4].summary)
            assertEquals(DevelopmentTaskStatus.REVIEW_REQUIRED, events[6].toStatus)
            assertEquals("implementation ready", events[6].summary)
            assertEquals(DevelopmentTaskStatus.COMPLETED, events.last().toStatus)
        } finally {
            prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun completed_task_cannot_be_reopened() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").remove("events").remove("events.corrupt_backup").commit()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("task-terminal", "ctx-a", "Terminal test"))
            store.transition("ctx-a", "task-terminal", DevelopmentTaskStatus.READY, 1L)
            store.transition("ctx-a", "task-terminal", DevelopmentTaskStatus.RUNNING, 2L)
            store.transition("ctx-a", "task-terminal", DevelopmentTaskStatus.REVIEW_REQUIRED, 3L, resultSummary = "ready")
            store.transition("ctx-a", "task-terminal", DevelopmentTaskStatus.COMPLETED, 4L, resultSummary = "passed")
            store.transition("ctx-a", "task-terminal", DevelopmentTaskStatus.READY, 5L)
        } finally {
            prefs.edit().remove("tasks").remove("events").remove("tasks.corrupt_backup").remove("events.corrupt_backup").commit()
        }
    }

    @Test
    fun review_handoff_requires_nonblank_summary_and_does_not_change_state() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").remove("events").remove("tasks.corrupt_backup").remove("events.corrupt_backup").commit()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("task-review-required", "ctx-a", "Review summary test"))
            store.transition("ctx-a", "task-review-required", DevelopmentTaskStatus.READY, 1L)
            store.transition("ctx-a", "task-review-required", DevelopmentTaskStatus.RUNNING, 2L)
            var rejected = false
            try {
                store.transition("ctx-a", "task-review-required", DevelopmentTaskStatus.REVIEW_REQUIRED, 3L)
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertEquals(true, rejected)
            assertEquals(DevelopmentTaskStatus.RUNNING, store.get("ctx-a", "task-review-required")?.status)
            assertEquals(3, store.events("ctx-a", "task-review-required").size)
        } finally {
            prefs.edit().remove("tasks").remove("events").remove("tasks.corrupt_backup").remove("events.corrupt_backup").commit()
        }
    }

    @Test
    fun update_timestamp_never_moves_backwards() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").commit()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("task-time", "ctx-a", "Timestamp test", createdAtMillis = 100L))
            val updated = store.transition("ctx-a", "task-time", DevelopmentTaskStatus.READY, 10L)
            assertEquals(100L, updated.updatedAtMillis)
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

    @Test
    fun separate_store_instances_do_not_lose_concurrent_task_creates() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
        val workers = 6
        val tasksPerWorker = 8
        val ready = java.util.concurrent.CountDownLatch(workers)
        val start = java.util.concurrent.CountDownLatch(1)
        val failures = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
        val threads = (0 until workers).map { worker ->
            Thread {
                try {
                    val store = DevelopmentTaskStore(context)
                    ready.countDown()
                    start.await()
                    repeat(tasksPerWorker) { index ->
                        val id = "concurrent-$worker-$index"
                        store.create(DevelopmentTask(id, "ctx-concurrent", id, createdAtMillis = 1L))
                    }
                } catch (error: Throwable) {
                    failures.add(error)
                }
            }
        }
        try {
            threads.forEach { it.start() }
            ready.await()
            start.countDown()
            threads.forEach { it.join() }
            assertEquals(emptyList<Throwable>(), failures.toList())
            val store = DevelopmentTaskStore(context)
            assertEquals(workers * tasksPerWorker, store.all("ctx-concurrent").size)
            assertEquals(workers * tasksPerWorker, prefs.getString("events", "[]")!!.let { org.json.JSONArray(it).length() })
        } finally {
            prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
        }
    }


    @Test
    fun task_duty_persists_across_store_instances() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
        try {
            DevelopmentTaskStore(context).create(
                DevelopmentTask("task-duty", "ctx-duty", "Duty persistence", duty = DevelopmentTaskDuty.REVIEW)
            )
            assertEquals(
                DevelopmentTaskDuty.REVIEW,
                DevelopmentTaskStore(context).get("ctx-duty", "task-duty")?.duty
            )
        } finally {
            prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
        }
    }

    @Test
    fun legacy_task_without_duty_defaults_to_build() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        val legacyTask = org.json.JSONObject().apply {
            put("id", "legacy-task")
            put("contextId", "ctx-legacy")
            put("title", "Legacy task")
            put("description", "")
            put("status", DevelopmentTaskStatus.CREATED.name)
            put("attempt", 0)
            put("resultSummary", "")
            put("errorMessage", "")
            put("createdAtMillis", 1L)
            put("updatedAtMillis", 1L)
        }
        prefs.edit().putString("tasks", org.json.JSONArray().put(legacyTask).toString())
            .remove("events").remove("tasks.corrupt_backup").remove("events.corrupt_backup").commit()
        try {
            assertEquals(
                DevelopmentTaskDuty.BUILD,
                DevelopmentTaskStore(context).get("ctx-legacy", "legacy-task")?.duty
            )
        } finally {
            prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
        }
    }

    @Test
    fun running_task_cannot_be_deleted_until_execution_is_resolved() {
        val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
        try {
            val store = DevelopmentTaskStore(context)
            store.create(DevelopmentTask("task-running-delete", "ctx-running-delete", "Running task"))
            store.transition("ctx-running-delete", "task-running-delete", DevelopmentTaskStatus.READY, 1L)
            store.transition("ctx-running-delete", "task-running-delete", DevelopmentTaskStatus.RUNNING, 2L)

            var rejected = false
            try {
                store.delete("ctx-running-delete", "task-running-delete")
            } catch (_: IllegalArgumentException) {
                rejected = true
            }

            assertEquals(true, rejected)
            assertEquals(
                DevelopmentTaskStatus.RUNNING,
                store.get("ctx-running-delete", "task-running-delete")?.status
            )
            store.transition("ctx-running-delete", "task-running-delete", DevelopmentTaskStatus.CANCELLED, 3L)
            store.delete("ctx-running-delete", "task-running-delete")
            assertNull(store.get("ctx-running-delete", "task-running-delete"))
            assertEquals(emptyList<DevelopmentTaskEvent>(), store.events("ctx-running-delete", "task-running-delete"))
        } finally {
            prefs.edit().remove("tasks").remove("tasks.corrupt_backup").remove("events").remove("events.corrupt_backup").commit()
        }
    }

}
