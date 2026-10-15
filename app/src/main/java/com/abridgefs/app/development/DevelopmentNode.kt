package com.aps.development

/** Context-scoped structure node. Identity is stable across renames and moves. */
data class DevelopmentNode(
    val id: String,
    val contextId: String,
    val code: String,
    val type: String,
    val name: String,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val notes: String = ""
) {
    init {
        require(id.isNotBlank()) { "节点 ID 不能为空" }
        require(contextId.isNotBlank()) { "Context ID 不能为空" }
        require(code.isNotBlank()) { "节点编码不能为空" }
        require(type.isNotBlank()) { "节点类型不能为空" }
        require(name.isNotBlank()) { "节点名称不能为空" }
        require(parentId?.isNotBlank() != false) { "父节点 ID 不能为空白" }
        require(parentId != id) { "节点不能以自身为父节点" }
    }
}
