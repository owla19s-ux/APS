package com.aps.development

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local persistence for development nodes and relations.
 * Task execution, connectors and Verify conclusions are deliberately out of scope.
 */
class DevelopmentTreeStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun allNodes(contextId: String): List<DevelopmentNode> =
        readNodes().filter { it.contextId == contextId }
            .sortedWith(compareBy<DevelopmentNode> { it.sortOrder }.thenBy { it.id })

    fun getNode(contextId: String, id: String): DevelopmentNode? =
        readNodes().firstOrNull { it.contextId == contextId && it.id == id }

    /** Root nodes, ordered for deterministic tree rendering. */
    fun roots(contextId: String): List<DevelopmentNode> =
        allNodes(contextId).filter { it.parentId == null }

    /** Direct children only; callers can build a partial tree without loading unrelated Contexts. */
    fun childrenOf(contextId: String, parentId: String): List<DevelopmentNode> =
        allNodes(contextId).filter { it.parentId == parentId }

    /** Build a UI-ready tree projection from the persisted parent-child relation. */
    fun tree(contextId: String): List<DevelopmentTreeBranch> {
        val nodes = allNodes(contextId)
        val byParent = nodes.groupBy { it.parentId }
        fun branch(node: DevelopmentNode): DevelopmentTreeBranch =
            DevelopmentTreeBranch(node, (byParent[node.id] ?: emptyList()).map(::branch))
        return (byParent[null] ?: emptyList()).map(::branch)
    }

    fun relationsFrom(contextId: String, nodeId: String): List<DevelopmentRelation> =
        allRelations(contextId).filter { it.sourceId == nodeId }

    fun relationsTo(contextId: String, nodeId: String): List<DevelopmentRelation> =
        allRelations(contextId).filter { it.targetId == nodeId }

    @Synchronized
    fun saveNode(node: DevelopmentNode) {
        val nodes = readNodes().filterNot { it.id == node.id } + node
        validateNodes(nodes)
        writeNodes(nodes)
    }

    @Synchronized
    fun moveNode(contextId: String, nodeId: String, newParentId: String?) {
        val nodes = readNodes()
        val node = nodes.firstOrNull { it.contextId == contextId && it.id == nodeId }
            ?: error("节点不存在")
        if (newParentId != null) {
            val parent = nodes.firstOrNull { it.contextId == contextId && it.id == newParentId }
                ?: error("目标父节点不存在或不属于当前 Context")
            require(parent.id != node.id) { "节点不能移动到自身之下" }
            var ancestor: DevelopmentNode? = parent
            while (ancestor != null) {
                require(ancestor.id != node.id) { "不能移动到自己的后代节点之下" }
                ancestor = ancestor.parentId?.let { id -> nodes.firstOrNull { it.contextId == contextId && it.id == id } }
            }
        }
        val updated = nodes.map { if (it.id == nodeId) it.copy(parentId = newParentId) else it }
        validateNodes(updated)
        writeNodes(updated)
    }

    @Synchronized
    fun deleteNode(contextId: String, id: String) {
        val nodes = readNodes()
        require(nodes.none { it.contextId == contextId && it.parentId == id }) { "请先移动或删除子节点" }
        if (nodes.none { it.contextId == contextId && it.id == id }) return
        writeNodes(nodes.filterNot { it.contextId == contextId && it.id == id })
        writeRelations(readRelations().filterNot {
            it.contextId == contextId && (it.sourceId == id || it.targetId == id)
        })
        DevelopmentTaskLinkStore(appContext).deleteForNode(contextId, id)
    }

    fun allRelations(contextId: String): List<DevelopmentRelation> =
        readRelations().filter { it.contextId == contextId }
            .sortedWith(compareBy<DevelopmentRelation> { it.relationType }.thenBy { it.id })

    @Synchronized
    fun saveRelation(relation: DevelopmentRelation) {
        val nodes = readNodes()
        require(nodes.any { it.contextId == relation.contextId && it.id == relation.sourceId }) {
            "关系源节点不存在或不属于当前 Context"
        }
        require(nodes.any { it.contextId == relation.contextId && it.id == relation.targetId }) {
            "关系目标节点不存在或不属于当前 Context"
        }
        val relations = readRelations().filterNot {
            it.id == relation.id || it.uniquenessKey == relation.uniquenessKey
        } + relation
        writeRelations(relations)
    }

    @Synchronized
    fun deleteRelation(contextId: String, id: String) {
        writeRelations(readRelations().filterNot { it.contextId == contextId && it.id == id })
    }

    private fun validateNodes(nodes: List<DevelopmentNode>) {
        require(nodes.groupBy { it.id }.values.all { it.size == 1 }) { "节点 ID 重复" }
        val byId = nodes.associateBy { it.id }
        nodes.forEach { node ->
            node.parentId?.let { parentId ->
                val parent = byId[parentId] ?: error("父节点不存在")
                require(parent.contextId == node.contextId) { "父节点必须属于同一 Context" }
                var ancestor: DevelopmentNode? = parent
                val visited = mutableSetOf<String>()
                while (ancestor != null) {
                    require(visited.add(ancestor.id)) { "检测到节点层级循环" }
                    require(ancestor.id != node.id) { "不能形成节点层级循环" }
                    ancestor = ancestor.parentId?.let { byId[it] }
                }
            }
        }
    }

    private fun readNodes(): List<DevelopmentNode> = readArray(KEY_NODES) { json ->
        DevelopmentNode(
            id = json.getString("id"), contextId = json.getString("contextId"),
            code = json.getString("code"), type = json.getString("type"),
            name = json.getString("name"), parentId = json.optString("parentId").ifBlank { null },
            sortOrder = json.optInt("sortOrder", 0), notes = json.optString("notes")
        )
    }

    private fun writeNodes(nodes: List<DevelopmentNode>) {
        val array = JSONArray()
        nodes.forEach { n -> array.put(JSONObject().apply {
            put("id", n.id); put("contextId", n.contextId); put("code", n.code)
            put("type", n.type); put("name", n.name); n.parentId?.let { put("parentId", it) }
            put("sortOrder", n.sortOrder); put("notes", n.notes)
        }) }
        check(prefs.edit().putString(KEY_NODES, array.toString()).commit()) { "节点保存失败" }
    }

    private fun readRelations(): List<DevelopmentRelation> = readArray(KEY_RELATIONS) { json ->
        DevelopmentRelation(
            id = json.getString("id"), contextId = json.getString("contextId"),
            sourceId = json.getString("sourceId"), targetId = json.getString("targetId"),
            relationType = json.getString("relationType"), notes = json.optString("notes")
        )
    }

    private fun writeRelations(relations: List<DevelopmentRelation>) {
        val array = JSONArray()
        relations.forEach { r -> array.put(JSONObject().apply {
            put("id", r.id); put("contextId", r.contextId); put("sourceId", r.sourceId)
            put("targetId", r.targetId); put("relationType", r.relationType); put("notes", r.notes)
        }) }
        check(prefs.edit().putString(KEY_RELATIONS, array.toString()).commit()) { "关系保存失败" }
    }

    private fun <T> readArray(key: String, decode: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList { for (i in 0 until array.length()) add(decode(array.getJSONObject(i))) }
        } catch (_: Exception) {
            val backupKey = "$key.corrupt_backup"
            if (!prefs.contains(backupKey)) prefs.edit().putString(backupKey, raw).commit()
            emptyList()
        }
    }

    private companion object {
        const val PREFERENCES = "aps_development_tree"
        const val KEY_NODES = "nodes"
        const val KEY_RELATIONS = "relations"
    }
}
