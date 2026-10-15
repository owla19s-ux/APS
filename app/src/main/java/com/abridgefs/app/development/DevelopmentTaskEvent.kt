package com.aps.development

/** Append-only record of a persisted Task state transition. */
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
        require(id.isNotBlank() && contextId.isNotBlank() && taskId.isNotBlank()) {
            "Task 事件身份字段不能为空"
        }
        require(occurredAtMillis >= 0L) { "事件时间不能为负数" }
        require(fromStatus != toStatus) { "事件不能记录无状态变化" }
    }
}
