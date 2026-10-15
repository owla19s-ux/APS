package com.aps.development

/** Snapshot of an actual GitHub Actions run associated with one APS Task. */
data class DevelopmentVerification(
    val id: String,
    val contextId: String,
    val taskId: String,
    val workflowRunId: Long,
    val runNumber: Long,
    val workflowName: String,
    val workflowUrl: String,
    val status: String,
    val conclusion: String?,
    val branch: String,
    val headSha: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    init {
        require(id.isNotBlank() && contextId.isNotBlank() && taskId.isNotBlank()) {
            "验证记录身份字段不能为空"
        }
        require(workflowRunId > 0L && runNumber > 0L) { "Workflow 运行 ID/编号必须为正数" }
        require(workflowName.isNotBlank()) { "Workflow 名称不能为空" }
        require(workflowUrl.startsWith("https://github.com/")) { "验证链接必须是 GitHub HTTPS 地址" }
        require(status in setOf("queued", "in_progress", "completed")) { "不支持的 Workflow 状态" }
        require(status != "completed" || !conclusion.isNullOrBlank()) {
            "已结束的 Workflow 必须包含真实结论"
        }
        require(headSha.isBlank() || headSha.matches(Regex("^[0-9a-fA-F]{40}$"))) {
            "提交 SHA 必须为空或完整的 40 位十六进制值"
        }
        require(createdAtMillis >= 0L && updatedAtMillis >= createdAtMillis) {
            "验证时间戳无效"
        }
    }

    val isSuccessful: Boolean
        get() = status == "completed" && conclusion == "success"
}
