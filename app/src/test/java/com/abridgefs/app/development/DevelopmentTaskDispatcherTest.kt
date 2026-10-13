package com.owla19s.aps.development

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DevelopmentTaskDispatcherTest {
    @Test
    fun ready_queue_is_context_scoped_and_oldest_first() {
        val tasks = listOf(
            DevelopmentTask("later", "ctx-a", "Later", status = DevelopmentTaskStatus.READY, createdAtMillis = 20L),
            DevelopmentTask("running", "ctx-a", "Running", status = DevelopmentTaskStatus.RUNNING, createdAtMillis = 1L),
            DevelopmentTask("other-context", "ctx-b", "Other", status = DevelopmentTaskStatus.READY, createdAtMillis = 2L),
            DevelopmentTask("earlier", "ctx-a", "Earlier", status = DevelopmentTaskStatus.READY, createdAtMillis = 10L),
            DevelopmentTask("created", "ctx-a", "Created", status = DevelopmentTaskStatus.CREATED, createdAtMillis = 0L)
        )

        assertEquals(
            listOf("earlier", "later"),
            DevelopmentTaskDispatcher.readyQueue(tasks, "ctx-a").map { it.id }
        )
        assertEquals("earlier", DevelopmentTaskDispatcher.nextReady(tasks, "ctx-a")?.id)
    }

    @Test
    fun ready_queue_can_filter_by_duty_without_changing_task_state() {
        val split = DevelopmentTask(
            "split", "ctx-a", "Split", duty = DevelopmentTaskDuty.SPLIT,
            status = DevelopmentTaskStatus.READY, createdAtMillis = 1L
        )
        val build = DevelopmentTask(
            "build", "ctx-a", "Build", duty = DevelopmentTaskDuty.BUILD,
            status = DevelopmentTaskStatus.READY, createdAtMillis = 2L
        )

        assertEquals(
            listOf("split"),
            DevelopmentTaskDispatcher.readyQueue(listOf(build, split), "ctx-a", DevelopmentTaskDuty.SPLIT).map { it.id }
        )
        assertEquals(DevelopmentTaskStatus.READY, split.status)
        assertNull(DevelopmentTaskDispatcher.nextReady(listOf(split), "ctx-a", DevelopmentTaskDuty.REVIEW))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blank_context_is_rejected() {
        DevelopmentTaskDispatcher.readyQueue(emptyList(), " ")
    }
}
