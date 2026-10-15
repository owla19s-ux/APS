package com.aps.development

/** Explicit cross-node relation, independent from tree parentage. */
data class DevelopmentRelation(
    val id: String,
    val contextId: String,
    val sourceId: String,
    val targetId: String,
    val relationType: String,
    val notes: String = ""
) {
    init {
        require(id.isNotBlank()) { "关系 ID 不能为空" }
        require(contextId.isNotBlank()) { "Context ID 不能为空" }
        require(sourceId.isNotBlank()) { "源节点 ID 不能为空" }
        require(targetId.isNotBlank()) { "目标节点 ID 不能为空" }
        require(sourceId != targetId) { "不允许节点关联自身" }
        require(relationType.isNotBlank()) { "关系类型不能为空" }
    }

    val uniquenessKey: String
        get() = "$contextId:$sourceId:$targetId:$relationType"
}
