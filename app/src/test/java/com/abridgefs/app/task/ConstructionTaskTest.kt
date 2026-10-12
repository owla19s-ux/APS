package com.owla19s.aps.task

import org.junit.Assert.assertEquals
import org.junit.Test

class ConstructionTaskTest {
    @Test
    fun task_starts_created_with_no_attempts() {
        val task = ConstructionTask("task-1", "ctx-1", "Implement feature", createdAt = 10L)
        assertEquals(ConstructionTaskStatus.CREATED, task.status)
        assertEquals(0, task.attempt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun failed_task_requires_error_summary() {
        ConstructionTask("task-1", "ctx-1", "Implement feature", status = ConstructionTaskStatus.FAILED, createdAt = 10L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun event_cannot_record_same_state_as_transition() {
        ConstructionTaskEvent("event-1", "ctx-1", "task-1", ConstructionTaskStatus.READY, ConstructionTaskStatus.READY, 20L)
    }
}
