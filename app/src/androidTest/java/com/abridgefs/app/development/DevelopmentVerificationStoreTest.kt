package com.owla19s.aps.development

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentVerificationStoreTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun observed_workflow_run_persists_and_cannot_rewrite_completed_conclusion() {
        val taskPrefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        val verifyPrefs = context.getSharedPreferences("aps_development_verifications", Context.MODE_PRIVATE)
        taskPrefs.edit().remove("tasks").remove("events").commit()
        verifyPrefs.edit().remove("records").remove("records.corrupt_backup").commit()
        try {
            DevelopmentTaskStore(context).create(
                DevelopmentTask("verify-task", "ctx-verify", "Verify Task", createdAtMillis = 100L)
            )
            val store = DevelopmentVerificationStore(context)
            store.save(record("queued", null, 100L, 110L))
            store.save(record("in_progress", null, 100L, 120L))
            store.save(record("completed", "success", 100L, 200L))

            val reloaded = DevelopmentVerificationStore(context).forTask("ctx-verify", "verify-task")
            assertEquals(1, reloaded.size)
            assertTrue(reloaded.single().isSuccessful)

            var rejected = false
            try {
                store.save(record("completed", "failure", 100L, 210L))
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue("Completed conclusion must be immutable", rejected)
            assertTrue(DevelopmentVerificationStore(context).forTask("ctx-verify", "verify-task").single().isSuccessful)
        } finally {
            taskPrefs.edit().remove("tasks").remove("events").remove("events.corrupt_backup").commit()
            verifyPrefs.edit().remove("records").remove("records.corrupt_backup").commit()
        }
    }

    @Test
    fun verification_requires_a_real_task_in_the_same_context() {
        val taskPrefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
        val verifyPrefs = context.getSharedPreferences("aps_development_verifications", Context.MODE_PRIVATE)
        taskPrefs.edit().remove("tasks").commit()
        verifyPrefs.edit().remove("records").commit()
        try {
            var rejected = false
            try {
                DevelopmentVerificationStore(context).save(record("completed", "success", 1L, 2L))
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue(rejected)
            assertFalse(DevelopmentVerificationStore(context).forTask("ctx-verify", "verify-task").isNotEmpty())
        } finally {
            taskPrefs.edit().remove("tasks").commit()
            verifyPrefs.edit().remove("records").commit()
        }
    }

    private fun record(
        status: String,
        conclusion: String?,
        createdAt: Long,
        updatedAt: Long
    ) = DevelopmentVerification(
        id = "verification-1",
        contextId = "ctx-verify",
        taskId = "verify-task",
        workflowRunId = 38234148573L,
        runNumber = 657L,
        workflowName = "Android Tests",
        workflowUrl = "https://github.com/owla19s-ux/APS/actions/runs/38234148573",
        status = status,
        conclusion = conclusion,
        branch = "main",
        headSha = "a".repeat(40),
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt
    )
}
