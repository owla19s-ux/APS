package com.aps.github

import com.google.gson.JsonObject

data class GitHubWorkflowRunSummary(
    val id: Long,
    val name: String,
    val branch: String,
    val status: String,
    val conclusion: String?,
    val event: String,
    val runNumber: Long,
    val htmlUrl: String
) {
    fun displayLabel(): String {
        val result = when {
            status == "in_progress" -> "运行中"
            status == "queued" -> "排队中"
            status == "completed" && conclusion == "success" -> "成功"
            status == "completed" && conclusion == "failure" -> "失败"
            status == "completed" && conclusion == "cancelled" -> "已取消"
            status == "completed" -> "已结束：${conclusion ?: "未知结果"}"
            else -> status
        }
        return "#${runNumber} · ${name} · ${result} · ${branch}"
    }

    companion object {
        fun parse(response: JsonObject): List<GitHubWorkflowRunSummary> {
            val runs = response.getAsJsonArray("workflow_runs")
                ?: throw IllegalStateException("GitHub 未返回工作流运行列表")
            return runs.mapNotNull { element ->
                val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val id = item.longOrNull("id") ?: return@mapNotNull null
                val runNumber = item.longOrNull("run_number") ?: id
                val name = item.stringOrNull("name")
                    ?: item.stringOrNull("display_title")
                    ?: "未命名工作流"
                val branch = item.stringOrNull("head_branch") ?: "未知分支"
                val status = item.stringOrNull("status") ?: "unknown"
                val conclusion = item.stringOrNull("conclusion")
                val event = item.stringOrNull("event") ?: "unknown"
                val htmlUrl = item.stringOrNull("html_url") ?: return@mapNotNull null
                if (!htmlUrl.startsWith("https://github.com/")) return@mapNotNull null
                GitHubWorkflowRunSummary(
                    id = id,
                    name = name,
                    branch = branch,
                    status = status,
                    conclusion = conclusion,
                    event = event,
                    runNumber = runNumber,
                    htmlUrl = htmlUrl
                )
            }
        }

        private fun JsonObject.stringOrNull(key: String): String? =
            get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrNull() }

        private fun JsonObject.longOrNull(key: String): Long? =
            get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asLong }.getOrNull() }
    }
}
