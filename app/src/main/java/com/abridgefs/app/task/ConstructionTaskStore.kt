package com.owla19s.aps.task

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local task lifecycle store. It records state only; it does not execute or dispatch work. */
class ConstructionTaskStore(context: Context) {
    private val prefs = context.getSharedPreferences("aps_construction_tasks", Context.MODE_PRIVATE)

    @Synchronized
    fun all(contextId: String): List<ConstructionTask> =
        readTasks().filter { it.contextId == contextId }.sortedWith(compareBy<ConstructionTask> { it.createdAt }.thenBy { it.id })

    @Synchronized
    fun get(contextId: String, taskId: String): ConstructionTask? =
        readTasks().firstOrNull { it.contextId == contextId && it.id == taskId }

    @Synchronized
    fun events(contextId: String, taskId: String): List<ConstructionTaskEvent> =
        readEvents().filter { it.contextId == contextId && it.taskId == taskId }.sortedBy { it.occurredAt }

    @Synchronized
    fun create(task: ConstructionTask) {
        require(task.status == ConstructionTaskStatus.CREATED && task.attempt == 0) { "新 Task 必须从 CREATED 开始" }
        require(readTasks().none { it.id == task.id }) { "Task ID 已存在" }
        writeTasks(readTasks() + task)
        appendEvent(ConstructionTaskEvent("created:" + task.id, task.contextId, task.id, null, task.status, task.createdAt, "created"))
    }

    @Synchronized
    fun markReady(contextId: String, taskId: String, now: Long) =
        transition(contextId, taskId, setOf(ConstructionTaskStatus.CREATED), ConstructionTaskStatus.READY, now)

    @Synchronized
    fun start(contextId: String, taskId: String, now: Long) =
        transition(contextId, taskId, setOf(ConstructionTaskStatus.READY), ConstructionTaskStatus.RUNNING, now, incrementAttempt = true)

    @Synchronized
    fun requestReview(contextId: String, taskId: String, now: Long, resultSummary: String) {
        require(resultSummary.isNotBlank()) { "执行结果摘要不能为空" }
        transition(contextId, taskId, setOf(ConstructionTaskStatus.RUNNING), ConstructionTaskStatus.REVIEW_REQUIRED, now, summary = resultSummary, resultSummary = resultSummary)
    }

    @Synchronized
    fun succeedAfterReview(contextId: String, taskId: String, now: Long, reviewSummary: String) {
        require(reviewSummary.isNotBlank()) { "审查结果摘要不能为空" }
        transition(contextId, taskId, setOf(ConstructionTaskStatus.REVIEW_REQUIRED), ConstructionTaskStatus.SUCCEEDED, now, summary = reviewSummary, resultSummary = reviewSummary)
    }

    @Synchronized
    fun fail(contextId: String, taskId: String, now: Long, errorSummary: String) {
        require(errorSummary.isNotBlank()) { "失败原因不能为空" }
        transition(contextId, taskId, setOf(ConstructionTaskStatus.RUNNING, ConstructionTaskStatus.REVIEW_REQUIRED), ConstructionTaskStatus.FAILED, now, summary = errorSummary, errorSummary = errorSummary)
    }

    @Synchronized
    fun retry(contextId: String, taskId: String, now: Long) =
        transition(contextId, taskId, setOf(ConstructionTaskStatus.FAILED), ConstructionTaskStatus.READY, now, clearResult = true)

    @Synchronized
    fun cancel(contextId: String, taskId: String, now: Long) =
        transition(contextId, taskId, setOf(ConstructionTaskStatus.CREATED, ConstructionTaskStatus.READY, ConstructionTaskStatus.RUNNING, ConstructionTaskStatus.REVIEW_REQUIRED), ConstructionTaskStatus.CANCELLED, now)

    private fun transition(
        contextId: String,
        taskId: String,
        allowed: Set<ConstructionTaskStatus>,
        nextStatus: ConstructionTaskStatus,
        now: Long,
        summary: String = "",
        resultSummary: String? = null,
        errorSummary: String? = null,
        incrementAttempt: Boolean = false,
        clearResult: Boolean = false
    ) {
        require(now >= 0L) { "时间不能为负数" }
        val tasks = readTasks().toMutableList()
        val index = tasks.indexOfFirst { it.contextId == contextId && it.id == taskId }
        require(index >= 0) { "Task 不存在或不属于当前 Context" }
        val old = tasks[index]
        require(old.status in allowed) { "非法 Task 状态转换：" + old.status + " -> " + nextStatus }
        val updated = old.copy(
            status = nextStatus,
            attempt = old.attempt + if (incrementAttempt) 1 else 0,
            updatedAt = maxOf(now, old.updatedAt),
            resultSummary = if (clearResult) "" else resultSummary ?: old.resultSummary,
            errorSummary = if (clearResult || nextStatus != ConstructionTaskStatus.FAILED) "" else errorSummary ?: old.errorSummary
        )
        tasks[index] = updated
        writeTasks(tasks)
        appendEvent(ConstructionTaskEvent(
            "event:" + taskId + ":" + now + ":" + nextStatus.name, contextId, taskId,
            old.status, nextStatus, maxOf(now, old.updatedAt), summary
        ))
    }

    private fun readTasks(): List<ConstructionTask> = readArray("tasks") { item ->
        ConstructionTask(
            item.getString("id"), item.getString("contextId"), item.getString("title"),
            item.optString("description"), ConstructionTaskStatus.valueOf(item.getString("status")),
            item.optInt("attempt"), item.getLong("createdAt"), item.getLong("updatedAt"),
            item.optString("resultSummary"), item.optString("errorSummary")
        )
    }

    private fun readEvents(): List<ConstructionTaskEvent> = readArray("events") { item ->
        ConstructionTaskEvent(
            item.getString("id"), item.getString("contextId"), item.getString("taskId"),
            if (item.isNull("fromStatus")) null else ConstructionTaskStatus.valueOf(item.getString("fromStatus")),
            ConstructionTaskStatus.valueOf(item.getString("toStatus")), item.getLong("occurredAt"),
            item.optString("summary")
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

    private fun writeTasks(tasks: List<ConstructionTask>) = writeArray("tasks", tasks.map { task ->
        JSONObject().apply {
            put("id", task.id); put("contextId", task.contextId); put("title", task.title)
            put("description", task.description); put("status", task.status.name); put("attempt", task.attempt)
            put("createdAt", task.createdAt); put("updatedAt", task.updatedAt)
            put("resultSummary", task.resultSummary); put("errorSummary", task.errorSummary)
        }
    })

    private fun appendEvent(event: ConstructionTaskEvent) {
        val events = readEvents()
        require(events.none { it.id == event.id }) { "Task 事件 ID 重复" }
        writeArray("events", (events + event).map { item ->
            JSONObject().apply {
                put("id", item.id); put("contextId", item.contextId); put("taskId", item.taskId)
                put("fromStatus", item.fromStatus?.name ?: JSONObject.NULL)
                put("toStatus", item.toStatus.name); put("occurredAt", item.occurredAt); put("summary", item.summary)
            }
        })
    }

    private fun writeArray(key: String, items: List<JSONObject>) {
        val array = JSONArray()
        items.forEach(array::put)
        check(prefs.edit().putString(key, array.toString()).commit()) { "Task 数据保存失败：" + key }
    }
}
