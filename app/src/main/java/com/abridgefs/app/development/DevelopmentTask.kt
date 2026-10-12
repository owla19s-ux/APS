package com.owla19s.aps.development

/** Minimal task hand-off record; this is not a Dispatcher or AI-agent runtime. */
data class DevelopmentTask(
    val id: String,
    val contextId: String,
    val title: String,
    val description: String = "",
    val status: DevelopmentTaskStatus = DevelopmentTaskStatus.CREATED,
    val attempt: Int = 0,
    val resultSummary: String = "",
    val errorMessage: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = createdAtMillis
) {
    init {
        require(id.isNotBlank()) { "Task ID 不能为空" }
        require(contextId.isNotBlank()) { "Context ID 不能为空" }
        require(title.isNotBlank()) { "Task 标题不能为空" }
        require(attempt >= 0) { "执行次数不能为负数" }
        require(createdAtMillis >= 0L && updatedAtMillis >= 0L) { "时间戳不能为负数" }
    }
}

enum class DevelopmentTaskStatus {
    CREATED,
    READY,
    RUNNING,
    REVIEW_REQUIRED,
    COMPLETED,
    FAILED,
    CANCELLED
}
