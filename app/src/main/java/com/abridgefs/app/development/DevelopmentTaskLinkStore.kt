package com.aps.development

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists Task-to-node links independently from task lifecycle. */
class DevelopmentTaskLinkStore(context: Context) {
    private val prefs = context.getSharedPreferences("aps_development_task_links", Context.MODE_PRIVATE)
    private val taskPrefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
    private val tree = DevelopmentTreeStore(context)

    fun all(contextId: String): List<DevelopmentTaskLink> =
        read().filter { it.contextId == contextId }
            .sortedWith(compareBy<DevelopmentTaskLink> { it.taskId }.thenBy { it.nodeId }.thenBy { it.relationType })

    fun forTask(contextId: String, taskId: String) = all(contextId).filter { it.taskId == taskId }
    fun forNode(contextId: String, nodeId: String) = all(contextId).filter { it.nodeId == nodeId }

    @Synchronized
    fun save(link: DevelopmentTaskLink) {
        require(taskExists(link.contextId, link.taskId)) { "施工卡不存在或不属于当前 Context" }
        require(tree.getNode(link.contextId, link.nodeId) != null) { "结构节点不存在或不属于当前 Context" }
        val next = read().filterNot { it.id == link.id || it.uniquenessKey == link.uniquenessKey } + link
        write(next)
    }

    @Synchronized
    fun delete(contextId: String, linkId: String) =
        write(read().filterNot { it.contextId == contextId && it.id == linkId })

    @Synchronized
    fun deleteForNode(contextId: String, nodeId: String) =
        write(read().filterNot { it.contextId == contextId && it.nodeId == nodeId })

    @Synchronized
    fun deleteForTask(contextId: String, taskId: String) =
        write(read().filterNot { it.contextId == contextId && it.taskId == taskId })

    private fun taskExists(contextId: String, taskId: String): Boolean {
        val raw = taskPrefs.getString("tasks", null) ?: return false
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).any { index ->
                val task = array.getJSONObject(index)
                task.optString("contextId") == contextId && task.optString("id") == taskId
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun read(): List<DevelopmentTaskLink> {
        val raw = prefs.getString("links", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(DevelopmentTaskLink(
                        item.getString("id"), item.getString("contextId"),
                        item.getString("taskId"), item.getString("nodeId"),
                        item.optString("relationType", "implements"), item.optString("notes")
                    ))
                }
            }
        } catch (_: Exception) {
            if (!prefs.contains("links.corrupt_backup")) {
                prefs.edit().putString("links.corrupt_backup", raw).commit()
            }
            emptyList()
        }
    }

    private fun write(links: List<DevelopmentTaskLink>) {
        val array = JSONArray()
        links.forEach { link -> array.put(JSONObject().apply {
            put("id", link.id); put("contextId", link.contextId); put("taskId", link.taskId)
            put("nodeId", link.nodeId); put("relationType", link.relationType); put("notes", link.notes)
        }) }
        check(prefs.edit().putString("links", array.toString()).commit()) { "施工卡关联保存失败" }
    }
}
