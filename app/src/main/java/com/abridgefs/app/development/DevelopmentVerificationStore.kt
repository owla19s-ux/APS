package com.owla19s.aps.development

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Stores observed verification runs; it never starts or fabricates a workflow result. */
class DevelopmentVerificationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val taskPrefs = context.applicationContext.getSharedPreferences("aps_development_tasks", Context.MODE_PRIVATE)

    fun all(contextId: String): List<DevelopmentVerification> =
        read().filter { it.contextId == contextId }.sortedWith(
            compareByDescending<DevelopmentVerification> { it.updatedAtMillis }.thenBy { it.workflowRunId }
        )

    fun forTask(contextId: String, taskId: String): List<DevelopmentVerification> =
        all(contextId).filter { it.taskId == taskId }

    @Synchronized
    fun save(record: DevelopmentVerification) {
        require(taskExists(record.contextId, record.taskId)) { "Task 不存在或不属于当前 Context" }
        val existing = read()
        val sameRun = existing.firstOrNull {
            it.contextId == record.contextId && it.taskId == record.taskId &&
                it.workflowRunId == record.workflowRunId
        }
        if (sameRun != null) {
            require(sameRun.id == record.id) { "同一 Task 的 Workflow 运行记录 ID 不一致" }
            require(record.updatedAtMillis >= sameRun.updatedAtMillis) { "验证记录更新时间不能倒退" }
            require(record.workflowUrl == sameRun.workflowUrl &&
                record.workflowName == sameRun.workflowName &&
                record.headSha == sameRun.headSha &&
                record.createdAtMillis == sameRun.createdAtMillis) { "同一 Workflow 运行的身份信息不能改变" }
            require(allowedUpdate(sameRun, record)) { "验证记录不能回退到较早的状态或覆盖已结束结论" }
        }
        val next = existing.filterNot {
            it.id == record.id ||
                (it.contextId == record.contextId && it.taskId == record.taskId &&
                    it.workflowRunId == record.workflowRunId)
        } + record
        val array = JSONArray()
        next.forEach { item -> array.put(item.toJson()) }
        check(prefs.edit().putString(KEY_RECORDS, array.toString()).commit()) { "验证记录保存失败" }
    }

    private fun allowedUpdate(old: DevelopmentVerification, new: DevelopmentVerification): Boolean {
        if (old.status == "completed") return old == new
        if (old.status == "in_progress" && new.status == "queued") return false
        if (old.status == "queued" && new.status == "queued") return true
        if (old.status == "queued" && new.status in setOf("in_progress", "completed")) return true
        if (old.status == "in_progress" && new.status in setOf("in_progress", "completed")) return true
        return false
    }

    private fun taskExists(contextId: String, taskId: String): Boolean {
        val raw = taskPrefs.getString("tasks", null) ?: return false
        return try {
            val tasks = JSONArray(raw)
            (0 until tasks.length()).any { i ->
                val task = tasks.getJSONObject(i)
                task.optString("id") == taskId && task.optString("contextId") == contextId
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun read(): List<DevelopmentVerification> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList { for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i))) }
        } catch (_: Exception) {
            if (!prefs.contains(KEY_BACKUP)) prefs.edit().putString(KEY_BACKUP, raw).commit()
            emptyList()
        }
    }

    private fun DevelopmentVerification.toJson() = JSONObject().apply {
        put("id", id); put("contextId", contextId); put("taskId", taskId)
        put("workflowRunId", workflowRunId); put("runNumber", runNumber)
        put("workflowName", workflowName); put("workflowUrl", workflowUrl)
        put("status", status); put("conclusion", conclusion ?: JSONObject.NULL)
        put("branch", branch); put("headSha", headSha)
        put("createdAtMillis", createdAtMillis); put("updatedAtMillis", updatedAtMillis)
    }

    private fun fromJson(json: JSONObject) = DevelopmentVerification(
        id = json.getString("id"), contextId = json.getString("contextId"), taskId = json.getString("taskId"),
        workflowRunId = json.getLong("workflowRunId"), runNumber = json.getLong("runNumber"),
        workflowName = json.getString("workflowName"), workflowUrl = json.getString("workflowUrl"),
        status = json.getString("status"),
        conclusion = if (json.isNull("conclusion")) null else json.getString("conclusion"),
        branch = json.optString("branch"), headSha = json.optString("headSha"),
        createdAtMillis = json.getLong("createdAtMillis"), updatedAtMillis = json.getLong("updatedAtMillis")
    )

    private companion object {
        const val PREFERENCES = "aps_development_verifications"
        const val KEY_RECORDS = "records"
        const val KEY_BACKUP = "records.corrupt_backup"
    }
}
