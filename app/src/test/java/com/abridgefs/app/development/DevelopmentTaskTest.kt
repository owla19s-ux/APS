package com.aps.development

import org.junit.Assert.assertEquals
import org.junit.Test

class DevelopmentTaskTest {
    @Test
    fun task_defaults_to_created_without_attempts() {
        val task = DevelopmentTask("task-1", "ctx-1", "Implement feature")
        assertEquals(DevelopmentTaskStatus.CREATED, task.status)
        assertEquals(0, task.attempt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blank_task_id_is_rejected() {
        DevelopmentTask(" ", "ctx-1", "Implement feature")
    }

    @Test(expected = IllegalArgumentException::class)
    fun negative_attempt_is_rejected() {
        DevelopmentTask("task-1", "ctx-1", "Implement feature", attempt = -1)
    }
}
