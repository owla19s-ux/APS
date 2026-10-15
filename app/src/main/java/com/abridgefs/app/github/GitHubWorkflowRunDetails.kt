package com.aps.github

import com.google.gson.JsonObject

data class GitHubWorkflowRunDetails(
    val id: Long,
    val name: String,
    val runNumber: Long,
    val branch: String,
    val event: String,
    val status: String,
    val conclusion: String?,
    val headSha: String,
    val actor: String,
    val createdAt: String,
    val updatedAt: String,
    val htmlUrl: String
) {
    fun toDisplayText(): String = buildString {
        append("工作流：").append(name).append("\n")
        append("运行编号：#").append(runNumber).append("\n")
        append("运行 ID：").append(id).append("\n")
        append("状态：").append(statusLabel()).append("\n")
        append("分支：").append(branch).append("\n")
        append("触发方式：").append(event).append("\n")
        if (!conclusion.isNullOrBlank()) append("结论：").append(conclusion).append("\n")
        if (actor.isNotBlank()) append("触发者：").append(actor).append("\n")
        if (createdAt.isNotBlank()) append("开始时间：").append(createdAt).append("\n")
        if (updatedAt.isNotBlank()) append("更新时间：").append(updatedAt).append("\n")
        if (headSha.isNotBlank()) append("提交：").append(headSha)
    }.trim()

    private fun statusLabel(): String = when {
        status == "in_progress" -> "运行中"
        status == "queued" -> "排队中"
        status == "completed" && conclusion == "success" -> "成功"
        status == "completed" && conclusion == "failure" -> "失败"
        status == "completed" && conclusion == "cancelled" -> "已取消"
        status == "completed" -> "已结束"
        else -> status
    }

    companion object {
        fun parse(item: JsonObject): GitHubWorkflowRunDetails {
            fun string(key: String): String = item.get(key)
                ?.takeUnless { it.isJsonNull }
                ?.let { runCatching { it.asString }.getOrNull() }
                .orEmpty()
            fun long(key: String): Long? = item.get(key)
                ?.takeUnless { it.isJsonNull }
                ?.let { runCatching { it.asLong }.getOrNull() }

            val id = long("id") ?: throw IllegalStateException("GitHub Actions 运行记录缺少 ID")
            val url = string("html_url")
            require(url.startsWith("https://github.com/")) {
                "GitHub Actions 返回了无效的结果链接"
            }
            val actor = item.getAsJsonObject("actor")
                ?.get("login")
                ?.takeUnless { it.isJsonNull }
                ?.let { runCatching { it.asString }.getOrNull() }
                .orEmpty()
            return GitHubWorkflowRunDetails(
                id = id,
                name = string("name").ifBlank { "未命名工作流" },
                runNumber = long("run_number") ?: id,
                branch = string("head_branch").ifBlank { "未知分支" },
                event = string("event").ifBlank { "unknown" },
                status = string("status").ifBlank { "unknown" },
                conclusion = string("conclusion").ifBlank { null },
                headSha = string("head_sha"),
                actor = actor,
                createdAt = string("created_at"),
                updatedAt = string("updated_at"),
                htmlUrl = url
            )
        }
    }
}
