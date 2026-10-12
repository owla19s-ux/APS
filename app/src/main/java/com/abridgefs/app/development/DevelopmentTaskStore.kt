package com.owla19s.aps.development

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    fun events(contextId: String, taskId: String): List<DevelopmentTaskEvent> =
        readEvents().filter { it.contextId == contextId && it.taskId == taskId }
            .sortedWith(compareBy<DevelopmentTaskEvent> { it.occurredAtMillis }.thenBy { it.id })

    @Synchronized
    fun create(task: DevelopmentTask) {
        require(task.status == DevelopmentTaskStatus.CREATED) { "新建 Task 必须处于 CREATED 状态" }
        require(task.attempt == 0) { "新建 Task 的执行次数必须为 0" }
        require(readAll().none { it.id == task.id }) { "Task ID 已存在" }
        writeAll(readAll() + task)
        appendEvent(DevelopmentTaskEvent(
            "created:" + task.contextId + ":" + task.id, task.contextId, task.id,
            null, task.status, task.createdAtMillis, "created"
        ))
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
        if (target == DevelopmentTaskStatus.REVIEW_REQUIRED || target == DevelopmentTaskStatus.COMPLETED) {
            require(resultSummary.isNotBlank()) { "执行结果/审查摘要不能为空" }
        }
        if (target == DevelopmentTaskStatus.FAILED) {
            require(errorMessage.isNotBlank()) { "失败原因不能为空" }
        }
        val updated = current.copy(
            status = target,
            attempt = if (target == DevelopmentTaskStatus.RUNNING) current.attempt + 1 else current.attempt,
            resultSummary = when {
                target == DevelopmentTaskStatus.REVIEW_REQUIRED || target == DevelopmentTaskStatus.COMPLETED -> resultSummary
                target == DevelopmentTaskStatus.READY -> ""
                else -> current.resultSummary
            },
            errorMessage = if (target == DevelopmentTaskStatus.FAILED) errorMessage else "",
            updatedAtMillis = maxOf(nowMillis, current.updatedAtMillis)
        )
        writeAll(tasks.map { if (it.id == taskId && it.contextId == contextId) updated else it })
        appendEvent(DevelopmentTaskEvent(
            UUID.randomUUID().toString(), contextId, taskId, current.status, target,
            updated.updatedAtMillis, when (target) {
                DevelopmentTaskStatus.REVIEW_REQUIRED -> resultSummary
                DevelopmentTaskStatus.COMPLETED -> resultSummary
                DevelopmentTaskStatus.FAILED -> errorMessage
                else -> ""
            }
        ))
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
                DevelopmentTaskStatus.REVIEW_REQUIRED, DevelopmentTaskStatus.FAILED, DevelopmentTaskStatus.CANCELLED
            )
            DevelopmentTaskStatus.REVIEW_REQUIRED -> setOf(
                DevelopmentTaskStatus.COMPLETED, DevelopmentTaskStatus.FAILED, DevelopmentTaskStatus.CANCELLED
            )
            DevelopmentTaskStatus.FAILED -> setOf(DevelopmentTaskStatus.READY, DevelopmentTaskStatus.CANCELLED)
            DevelopmentTaskStatus.COMPLETED, DevelopmentTaskStatus.CANCELLED -> emptySet()
        }

    private fun readAll(): List<DevelopmentTask> = readArray("tasks") { item ->
        DevelopmentTask(
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
        )
    }

    private fun readEvents(): List<DevelopmentTaskEvent> = readArray("events") { item ->
        DevelopmentTaskEvent(
            id = item.getString("id"),
            contextId = item.getString("contextId"),
            taskId = item.getString("taskId"),
            fromStatus = if (item.isNull("fromStatus")) null else DevelopmentTaskStatus.valueOf(item.getString("fromStatus")),
            toStatus = DevelopmentTaskStatus.valueOf(item.getString("toStatus")),
            occurredAtMillis = item.getLong("occurredAtMillis"),
            summary = item.optString("summary")
        )
    }

    private fun <T> readArray(key: String, decode: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList { for (i in 0 until array.length()) add(decode(array.getJSONObject(i))) }
        } catch (_: Exception) {
            val backup = key + ".corrupt_backup"
            if (!prefs.contains(backup)) prefs.edit().putString(backup, raw).commit()
            emptyList()
        }
    }

    private fun appendEvent(event: DevelopmentTaskEvent) {
        val events = readEvents()
        require(events.none { it.id == event.id }) { "Task 事件 ID 重复" }
        writeArray("events", (events + event).map { item ->
            JSONObject().apply {
                put("id", item.id); put("contextId", item.contextId); put("taskId", item.taskId)
                put("fromStatus", item.fromStatus?.name ?: JSONObject.NULL)
                put("toStatus", item.toStatus.name); put("occurredAtMillis", item.occurredAtMillis)
                put("summary", item.summary)
            }
        })
    }

    private fun writeAll(tasks: List<DevelopmentTask>) {
        writeArray("tasks", tasks.map { task ->
            JSONObject().apply {
                put("id", task.id); put("contextId", task.contextId); put("title", task.title)
                put("description", task.description); put("status", task.status.name)
                put("attempt", task.attempt); put("resultSummary", task.resultSummary)
                put("errorMessage", task.errorMessage); put("createdAtMillis", task.createdAtMillis)
                put("updatedAtMillis", task.updatedAtMillis)
            }
        })
    }

    private fun writeArray(key: String, items: List<JSONObject>) {
        val array = JSONArray()
        items.forEach(array::put)
        check(prefs.edit().putString(key, array.toString()).commit()) { "Task 数据保存失败：$key" }
    }
}
