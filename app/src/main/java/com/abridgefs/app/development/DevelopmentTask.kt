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

/** Append-only record of an accepted task state transition. */
data class DevelopmentTaskEvent(
    val id: String,
    val contextId: String,
    val taskId: String,
    val fromStatus: DevelopmentTaskStatus?,
    val toStatus: DevelopmentTaskStatus,
    val occurredAtMillis: Long,
    val summary: String = ""
) {
    init {
        require(id.isNotBlank() && contextId.isNotBlank() && taskId.isNotBlank()) { "事件身份字段不能为空" }
        require(occurredAtMillis >= 0L) { "事件时间不能为负数" }
        require(fromStatus != toStatus) { "事件不能记录无状态变化" }
    }
}
