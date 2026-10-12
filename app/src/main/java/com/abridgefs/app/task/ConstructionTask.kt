package com.owla19s.aps.task

/** Lifecycle state for a construction card. Execution is performed by a later dispatcher layer. */
enum class ConstructionTaskStatus {
    CREATED, READY, RUNNING, REVIEW_REQUIRED, SUCCEEDED, FAILED, CANCELLED
}

/** Independent task identity and lifecycle; development-node links live in their own relation store. */
data class ConstructionTask(
    val id: String,
    val contextId: String,
    val title: String,
    val description: String = "",
    val status: ConstructionTaskStatus = ConstructionTaskStatus.CREATED,
    val attempt: Int = 0,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val resultSummary: String = "",
    val errorSummary: String = ""
) {
    init {
        require(id.isNotBlank()) { "Task ID 不能为空" }
        require(contextId.isNotBlank()) { "Context ID 不能为空" }
        require(title.isNotBlank()) { "Task 标题不能为空" }
        require(attempt >= 0) { "执行次数不能为负数" }
        require(createdAt >= 0L && updatedAt >= createdAt) { "Task 时间字段无效" }
        require(status != ConstructionTaskStatus.FAILED || errorSummary.isNotBlank()) {
            "失败状态必须记录错误摘要"
        }
    }
}

/** Append-only lifecycle receipt for auditing state transitions. */
data class ConstructionTaskEvent(
    val id: String,
    val contextId: String,
    val taskId: String,
    val fromStatus: ConstructionTaskStatus?,
    val toStatus: ConstructionTaskStatus,
    val occurredAt: Long,
    val summary: String = ""
) {
    init {
        require(id.isNotBlank() && contextId.isNotBlank() && taskId.isNotBlank()) { "事件身份字段不能为空" }
        require(occurredAt >= 0L) { "事件时间不能为负数" }
        require(fromStatus != toStatus) { "事件不能记录无状态变化" }
    }
}
