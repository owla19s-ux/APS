package com.abridgefs.app.github

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubWorkflowRunDetailsTest {
    @Test
    fun parsesRunEvidenceFieldsForDisplay() {
        val item = JsonObject().apply {
            addProperty("id", 77L)
            addProperty("run_number", 12L)
            addProperty("name", "Android Tests")
            addProperty("head_branch", "main")
            addProperty("event", "push")
            addProperty("status", "completed")
            addProperty("conclusion", "success")
            addProperty("head_sha", "abcdef123456")
            addProperty("created_at", "2026-10-12T04:00:00Z")
            addProperty("updated_at", "2026-10-12T04:02:00Z")
            addProperty("html_url", "https://github.com/owner/repo/actions/runs/77")
            add("actor", JsonObject().apply { addProperty("login", "builder") })
        }

        val details = GitHubWorkflowRunDetails.parse(item)
        val display = details.toDisplayText()

        assertEquals(77L, details.id)
        assertEquals(12L, details.runNumber)
        assertEquals("abcdef123456", details.headSha)
        assertEquals("builder", details.actor)
        assertTrue(display.contains("成功"))
        assertTrue(display.contains("abcdef123456"))
        assertTrue(display.contains("builder"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonGitHubResultUrl() {
        GitHubWorkflowRunDetails.parse(JsonObject().apply {
            addProperty("id", 77L)
            addProperty("html_url", "https://example.com/not-github")
        })
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsRunWithoutId() {
        GitHubWorkflowRunDetails.parse(JsonObject().apply {
            addProperty("html_url", "https://github.com/owner/repo/actions/runs/77")
        })
    }
}
