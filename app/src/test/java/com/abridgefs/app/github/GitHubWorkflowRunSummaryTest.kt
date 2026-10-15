package com.aps.github

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubWorkflowRunSummaryTest {
    @Test
    fun parsesActualRunFieldsAndMapsSuccessStatus() {
        val response = JsonObject().apply {
            add("workflow_runs", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("id", 123L)
                    addProperty("run_number", 45L)
                    addProperty("name", "Android Tests")
                    addProperty("head_branch", "main")
                    addProperty("status", "completed")
                    addProperty("conclusion", "success")
                    addProperty("event", "push")
                    addProperty("html_url", "https://github.com/owla19s-ux/APS/actions/runs/123")
                })
            })
        }

        val result = GitHubWorkflowRunSummary.parse(response).single()

        assertEquals(123L, result.id)
        assertEquals(45L, result.runNumber)
        assertEquals("main", result.branch)
        assertEquals("push", result.event)
        assertTrue(result.displayLabel().contains("成功"))
        assertTrue(result.displayLabel().contains("Android Tests"))
    }

    @Test
    fun doesNotExposeNonGitHubLinksAsOpenableResults() {
        val response = JsonObject().apply {
            add("workflow_runs", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("id", 123L)
                    addProperty("html_url", "https://example.com/fake")
                })
            })
        }

        assertTrue(GitHubWorkflowRunSummary.parse(response).isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun missingWorkflowRunCollectionIsReportedAsMalformedResponse() {
        GitHubWorkflowRunSummary.parse(JsonObject())
    }
}
