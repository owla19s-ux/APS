package com.owla19s.aps.development

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local Task lifecycle store. State changes are validated here, not inferred from UI text. */
class DevelopmentTaskStore(context: Context) {
    private val prefs = context.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)
    private val links = DevelopmentTaskLinkStore(context)

    fun all(contextId: String): List<DevelopmentTask> =
        readAll().filter { it.contextId == contextId }
            .sortedWith(compareBy<DevelopmentTask> { it.createdAtMillis }.thenBy { it.id })

    fun get(contextId: String, taskId: String): DevelopmentTask? =
        readAll().firstOrNull { it.contextId == contextId && it.id == taskId }

    @Synchronized
    fun create(task: DevelopmentTask) {
        require(task.status == DevelopmentTaskStatus.CREATED) { "新建 Task 必须处于 CREATED 状态" }
        require(task.attempt == 0) { "新建 Task 的执行次数必须为 0" }
        require(readAll().none { it.id == task.id }) { "Task ID 已存在" }
        writeAll(readAll() + task)
    }

    @Synchronized
    fun transition(
        contextId: String,
        taskId: String,
        target: DevelopmentTaskStatus,
        nowMillis: Long,
        resultSummary: String = "",
        errorMessage: String = ""
    ): DevelopmentTask {
        require(nowMillis >= 0L) { "时间戳不能为负数" }
        val tasks = readAll()
        val current = tasks.firstOrNull { it.contextId == contextId && it.id == taskId }
            ?: error("Task 不存在或不属于当前 Context")
        require(target in allowedTransitions(current.status)) {
            "不允许的 Task 状态迁移：${current.status} → $target"
        }
        val attempt = when {
            target == DevelopmentTaskStatus.RUNNING -> current.attempt + 1
            else -> current.attempt
        }
        val updated = current.copy(
            status = target,
            attempt = attempt,
            resultSummary = if (target == DevelopmentTaskStatus.COMPLETED) resultSummary else current.resultSummary,
            errorMessage = if (target == DevelopmentTaskStatus.FAILED) errorMessage else "",
            updatedAtMillis = maxOf(nowMillis, current.updatedAtMillis)
        )
        writeAll(tasks.map { if (it.id == taskId && it.contextId == contextId) updated else it })
        return updated
    }

    @Synchronized
    fun delete(contextId: String, taskId: String) {
        val tasks = readAll()
        if (tasks.none { it.contextId == contextId && it.id == taskId }) return
        writeAll(tasks.filterNot { it.contextId == contextId && it.id == taskId })
        links.deleteForTask(contextId, taskId)
    }

    private fun allowedTransitions(status: DevelopmentTaskStatus): Set<DevelopmentTaskStatus> =
        when (status) {
            DevelopmentTaskStatus.CREATED -> setOf(DevelopmentTaskStatus.READY, DevelopmentTaskStatus.CANCELLED)
            DevelopmentTaskStatus.READY -> setOf(DevelopmentTaskStatus.RUNNING, DevelopmentTaskStatus.CANCELLED)
            DevelopmentTaskStatus.RUNNING -> setOf(
                DevelopmentTaskStatus.COMPLETED, DevelopmentTaskStatus.FAILED, DevelopmentTaskStatus.CANCELLED
            )
            DevelopmentTaskStatus.FAILED -> setOf(DevelopmentTaskStatus.READY, DevelopmentTaskStatus.CANCELLED)
            DevelopmentTaskStatus.COMPLETED, DevelopmentTaskStatus.CANCELLED -> emptySet()
        }

    private fun readAll(): List<DevelopmentTask> {
        val raw = prefs.getString("tasks", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(DevelopmentTask(
                        id = item.getString("id"),
                        contextId = item.getString("contextId"),
                        title = item.getString("title"),
                        description = item.optString("description"),
                        status = DevelopmentTaskStatus.valueOf(item.getString("status")),
                        attempt = item.optInt("attempt", 0),
                        resultSummary = item.optString("resultSummary"),
                        errorMessage = item.optString("errorMessage"),
                        createdAtMillis = item.optLong("createdAtMillis", 0L),
                        updatedAtMillis = item.optLong("updatedAtMillis", 0L)
                    ))
                }
            }
        } catch (_: Exception) {
            if (!prefs.contains("tasks.corrupt_backup")) {
                prefs.edit().putString("tasks.corrupt_backup", raw).commit()
            }
            emptyList()
        }
    }

    private fun writeAll(tasks: List<DevelopmentTask>) {
        val array = JSONArray()
        tasks.forEach { task -> array.put(JSONObject().apply {
            put("id", task.id)
            put("contextId", task.contextId)
            put("title", task.title)
            put("description", task.description)
            put("status", task.status.name)
            put("attempt", task.attempt)
            put("resultSummary", task.resultSummary)
            put("errorMessage", task.errorMessage)
            put("createdAtMillis", task.createdAtMillis)
            put("updatedAtMillis", task.updatedAtMillis)
        }) }
        check(prefs.edit().putString("tasks", array.toString()).commit()) { "Task 保存失败" }
    }
}
