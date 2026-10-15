package com.aps.development

/**
 * Explicit association between an independently identified construction task and a structure node.
 * A task may reference multiple nodes and a node may be referenced by multiple tasks.
 */
data class DevelopmentTaskLink(
    val id: String,
    val contextId: String,
    val taskId: String,
    val nodeId: String,
    val relationType: String = "implements",
    val notes: String = ""
) {
    init {
        require(id.isNotBlank()) { "关联 ID 不能为空" }
        require(contextId.isNotBlank()) { "Context ID 不能为空" }
        require(taskId.isNotBlank()) { "Task ID 不能为空" }
        require(nodeId.isNotBlank()) { "结构节点 ID 不能为空" }
        require(relationType.isNotBlank()) { "关联类型不能为空" }
    }

    val uniquenessKey: String
        get() = "$contextId:$taskId:$nodeId:$relationType"
}
