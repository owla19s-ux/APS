package com.owla19s.aps

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.owla19s.aps.context.ContextStore
import com.owla19s.aps.ai.AIConnectorRegistry
import com.owla19s.aps.ai.AIProfileStore
import com.owla19s.aps.development.DevelopmentTaskExecutionService
import com.owla19s.aps.development.DevelopmentNode
import com.owla19s.aps.development.DevelopmentTask
import com.owla19s.aps.development.DevelopmentTaskEvent
import com.owla19s.aps.development.DevelopmentTaskDuty
import com.owla19s.aps.development.DevelopmentTaskLink
import com.owla19s.aps.development.DevelopmentTaskLinkStore
import com.owla19s.aps.development.DevelopmentTaskStatus
import com.owla19s.aps.development.DevelopmentTaskStore
import com.owla19s.aps.development.DevelopmentTreeStore
import com.owla19s.aps.development.DevelopmentVerification
import com.owla19s.aps.development.DevelopmentVerificationStore
import java.text.DateFormat
import java.util.Date
import java.util.UUID

/** Context-scoped Task list and details. Completion and GitHub verification remain separate facts. */
class DevelopmentTaskActivity : AppCompatActivity() {
    private lateinit var contextId: String
    private var filterNodeId: String? = null
    private lateinit var tasks: DevelopmentTaskStore
    private lateinit var tree: DevelopmentTreeStore
    private lateinit var links: DevelopmentTaskLinkStore
    private lateinit var verifications: DevelopmentVerificationStore
    private lateinit var content: LinearLayout
    private val activeExecutionJobs = mutableMapOf<String, Job>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contextId = intent.getStringExtra(EXTRA_CONTEXT_ID).orEmpty()
        if (contextId.isBlank() || ContextStore(this).get(contextId) == null) {
            Toast.makeText(this, "Context 不存在或已删除", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        tasks = DevelopmentTaskStore(this)
        tree = DevelopmentTreeStore(this)
        val requestedNodeId = intent.getStringExtra(EXTRA_NODE_ID)
        if (!requestedNodeId.isNullOrBlank() && tree.getNode(contextId, requestedNodeId) == null) {
            Toast.makeText(this, "结构节点不存在或已删除", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        filterNodeId = requestedNodeId?.takeIf { it.isNotBlank() }
        links = DevelopmentTaskLinkStore(this)
        verifications = DevelopmentVerificationStore(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
            setBackgroundColor(Color.rgb(243, 246, 250))
        }
        setContentView(ScrollView(this).apply { addView(content) })
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) render()
    }

    private fun render() {
        content.removeAllViews()
        content.addView(TextView(this).apply {
            text = filterNodeId?.let { nodeId -> "关联施工卡：${tree.getNode(contextId, nodeId)?.name ?: nodeId}" } ?: "施工卡 / Task"
            textSize = 26f
            setTextColor(Color.rgb(28, 43, 61))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, params())
        content.addView(TextView(this).apply {
            text = "任务状态来自本地 Task 生命周期；GitHub Actions 验证结果单独记录，不会因 Task 完成而自动判定验证通过。"
            textSize = 13f
            setTextColor(Color.rgb(69, 88, 108))
            setPadding(0, dp(4), 0, dp(12))
        }, params())
        content.addView(Button(this).apply {
            text = "新建施工卡"
            setOnClickListener { createTask() }
        }, params())
        content.addView(Button(this).apply {
            text = "待调度队列"
            setOnClickListener { chooseReadyQueueDuty() }
        }, params())
        if (filterNodeId != null) content.addView(Button(this).apply {
            text = "查看全部施工卡"
            setOnClickListener { filterNodeId = null; render() }
        }, params())
        val relatedTaskIds = filterNodeId?.let { nodeId -> links.forNode(contextId, nodeId).map { it.taskId }.toSet() }
        val list = tasks.all(contextId).filter { relatedTaskIds == null || it.id in relatedTaskIds }
        if (list.isEmpty()) {
            content.addView(TextView(this).apply {
                text = if (filterNodeId != null) "此结构节点还没有关联施工卡。点击“新建施工卡”可直接创建并关联。" else "此 Context 尚无施工卡。新建后可关联结构节点，并记录状态、事件和已保存的验证结果。"
                textSize = 15f
                setPadding(dp(12), dp(14), dp(12), dp(14))
                setBackgroundColor(Color.WHITE)
            }, params())
        } else list.forEach { task ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(Color.WHITE)
            }
            card.addView(TextView(this).apply {
                text = task.title
                textSize = 17f
                setTextColor(Color.rgb(28, 43, 61))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = "职责：${task.duty.displayName} · 状态：${task.status.name} · 尝试次数 ${task.attempt}"
                textSize = 13f
                setTextColor(Color.rgb(69, 88, 108))
                setPadding(0, dp(3), 0, dp(3))
            })
            val taskLinks = links.forTask(contextId, task.id)
            val nodeNames = taskLinks.mapNotNull { link -> tree.getNode(contextId, link.nodeId)?.name }
            card.addView(TextView(this).apply {
                text = if (nodeNames.isEmpty()) "未关联结构节点" else "结构节点：${nodeNames.joinToString("、")}"
                textSize = 13f
                setTextColor(Color.rgb(95, 111, 130))
            })
            val latest = verifications.forTask(contextId, task.id).firstOrNull()
            card.addView(TextView(this).apply {
                text = if (latest == null) "GitHub Verify：暂无关联记录" else
                    "GitHub Verify：${latest.status}" + (latest.conclusion?.let { " / $it" } ?: "")
                textSize = 13f
                setTextColor(if (latest?.isSuccessful == true) Color.rgb(30, 120, 70) else Color.rgb(95, 111, 130))
            })
            card.setOnClickListener { showDetails(task.id) }
            content.addView(card, params().apply { bottomMargin = dp(8) })
        }
    }


    private fun chooseReadyQueueDuty() {
        val duties = listOf<DevelopmentTaskDuty?>(null) + DevelopmentTaskDuty.values().toList()
        val labels = listOf("全部职责") + DevelopmentTaskDuty.values().map { it.displayName }
        AlertDialog.Builder(this)
            .setTitle("按职责筛选待调度队列")
            .setItems(labels.toTypedArray()) { _, index -> showReadyQueue(duties[index]) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showReadyQueue(duty: DevelopmentTaskDuty?) {
        val queue = com.owla19s.aps.development.DevelopmentTaskDispatcher.readyQueue(
            tasks.all(contextId), contextId, duty
        )
        val dutyLabel = duty?.displayName ?: "全部职责"
        if (queue.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("待调度队列 · $dutyLabel")
                .setMessage("当前筛选下没有 READY 施工卡。此队列只筛选本地任务，不会自动启动 AI 或 GitHub Workflow。")
                .setPositiveButton("确定", null)
                .show()
            return
        }
        val labels = queue.map { task ->
            "${task.duty.displayName} · ${task.title} · ${formatTime(task.createdAtMillis)}"
        }
        AlertDialog.Builder(this)
            .setTitle("待调度队列 · $dutyLabel（${queue.size}）")
            .setItems(labels.toTypedArray()) { _, index -> showDetails(queue[index].id) }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun createTask() {
        val title = EditText(this).apply { hint = "施工卡名称（必填）" }
        val description = EditText(this).apply { hint = "任务说明（可留空）"; minLines = 3; gravity = Gravity.TOP }
        val dutyOptions = DevelopmentTaskDuty.values()
        val duty = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@DevelopmentTaskActivity,
                android.R.layout.simple_spinner_dropdown_item,
                dutyOptions.map { it.displayName }
            )
            setSelection(dutyOptions.indexOf(DevelopmentTaskDuty.BUILD))
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(title, params())
            addView(TextView(this@DevelopmentTaskActivity).apply {
                text = "职责（与 AI Code 权限等级分开）"
                textSize = 13f
                setTextColor(Color.rgb(69, 88, 108))
            }, params())
            addView(duty, params())
            addView(description, params())
        }
        AlertDialog.Builder(this).setTitle("新建施工卡").setView(form)
            .setNegativeButton("取消", null)
            .setPositiveButton("创建") { _, _ ->
                val name = title.text.toString().trim()
                if (name.isBlank()) { toast("施工卡名称不能为空"); return@setPositiveButton }
                val now = System.currentTimeMillis()
                try {
                    val created = DevelopmentTask(
                        id = UUID.randomUUID().toString(), contextId = contextId,
                        title = name, description = description.text.toString().trim(),
                        duty = dutyOptions[duty.selectedItemPosition],
                        status = DevelopmentTaskStatus.CREATED, createdAtMillis = now, updatedAtMillis = now
                    )
                    tasks.create(created)
                    try {
                        filterNodeId?.let { nodeId ->
                            links.save(DevelopmentTaskLink(
                                id = UUID.randomUUID().toString(), contextId = contextId,
                                taskId = created.id, nodeId = nodeId
                            ))
                        }
                    } catch (linkError: Exception) {
                        tasks.delete(contextId, created.id)
                        throw linkError
                    }
                    render()
                    toast(if (filterNodeId == null) "施工卡已创建" else "施工卡已创建并关联到结构节点")
                } catch (e: Exception) { toast("创建失败：${e.message ?: "未知错误"}") }
            }.show()
    }

    private fun showDetails(taskId: String) {
        val task = tasks.get(contextId, taskId) ?: return
        val taskLinks = links.forTask(contextId, taskId)
        val nodeMap = tree.allNodes(contextId).associateBy { it.id }
        val taskEvents = tasks.events(contextId, taskId)
        val taskVerifications = verifications.forTask(contextId, taskId)
        val details = buildString {
            appendLine(task.title)
            appendLine("职责：${task.duty.displayName}")
            appendLine("状态：${task.status.name} · 执行尝试：${task.attempt}")
            appendLine("创建：${formatTime(task.createdAtMillis)}")
            appendLine("更新：${formatTime(task.updatedAtMillis)}")
            if (task.description.isNotBlank()) appendLine("\n说明：\n${task.description}")
            if (task.resultSummary.isNotBlank()) appendLine("\n最近结果：\n${task.resultSummary}")
            if (task.errorMessage.isNotBlank()) appendLine("\n最近错误：\n${task.errorMessage}")
            appendLine("\n关联结构节点：")
            if (taskLinks.isEmpty()) appendLine("（无）") else taskLinks.forEach { link ->
                appendLine("• ${nodeMap[link.nodeId]?.name ?: link.nodeId} · ${link.relationType}")
            }
            appendLine("\n生命周期事件：")
            if (taskEvents.isEmpty()) appendLine("（无）") else taskEvents.forEach { event ->
                appendLine("• ${event.fromStatus?.name ?: "新建"} → ${event.toStatus.name} · ${formatTime(event.occurredAtMillis)}" +
                    if (event.summary.isBlank()) "" else " · ${event.summary}")
            }
            appendLine("\n已保存的 GitHub 验证记录：")
            if (taskVerifications.isEmpty()) appendLine("（无；此处不会自动查询 GitHub）") else taskVerifications.forEach { v ->
                appendLine("• #${v.runNumber} · ${v.status}" +
                    (v.conclusion?.let { " / $it" } ?: "") + " · ${v.branch}")
                appendLine("  ${v.workflowName}")
                appendLine("  ${v.workflowUrl}")
            }
        }
        val box = TextView(this).apply {
            text = details
            textSize = 14f
            setTextColor(Color.rgb(28, 43, 61))
            setPadding(dp(20), dp(12), dp(20), dp(12))
            setTextIsSelectable(true)
        }
        val actions = mutableListOf<String>()
        // While this Activity owns an AI execution, only its Job may cancel it.
        // Do not let a manual status transition race the service's final state transition.
        val allowed = if (task.status == DevelopmentTaskStatus.RUNNING && activeExecutionJobs.containsKey(task.id)) {
            listOf("取消 AI 执行")
        } else {
            allowedActions(task.status)
        }
        actions += allowed
        actions += "关联结构节点"
        if (taskLinks.isNotEmpty()) actions += "解除结构节点关联"
        if (task.status != DevelopmentTaskStatus.RUNNING) actions += "删除施工卡"
        AlertDialog.Builder(this).setTitle("施工卡详情").setView(ScrollView(this).apply { addView(box) })
            .setNeutralButton("关闭", null)
            .setItems(actions.toTypedArray()) { _, which ->
                when {
                    which < allowed.size -> {
                        when (allowed[which]) {
                            "交给 AI 执行" -> executeTask(task.id)
                            "取消 AI 执行" -> {
                                activeExecutionJobs[task.id]?.cancel()
                                toast("已请求取消 AI 执行")
                            }
                            else -> changeStatus(task, allowed[which])
                        }
                    }
                    which == allowed.size -> chooseNode(task.id)
                    taskLinks.isNotEmpty() && which == allowed.size + 1 -> chooseLinkToRemove(task.id)
                    else -> confirmDelete(task.id)
                }
            }.show()
    }

    private fun allowedActions(status: DevelopmentTaskStatus): List<String> = when (status) {
        DevelopmentTaskStatus.CREATED -> listOf("设为待执行", "取消任务")
        DevelopmentTaskStatus.READY -> listOf("交给 AI 执行", "取消任务")
        DevelopmentTaskStatus.RUNNING -> listOf("提交审查", "标记失败", "取消任务")
        DevelopmentTaskStatus.REVIEW_REQUIRED -> listOf("审查通过并完成", "标记失败", "取消任务")
        DevelopmentTaskStatus.FAILED -> listOf("重新设为待执行", "取消任务")
        DevelopmentTaskStatus.COMPLETED, DevelopmentTaskStatus.CANCELLED -> emptyList()
    }

    private fun executeTask(taskId: String) {
        val context = ContextStore(this).get(contextId)
        val connectionId = context?.aiConnectionId
        if (context == null || connectionId.isNullOrBlank()) {
            toast("当前 Context 尚未绑定 AI Connection")
            return
        }
        val profileStore = AIProfileStore(this)
        val profile = profileStore.load(connectionId)
        if (profile == null) {
            toast("找不到当前 Context 的 AI 配置，请检查连接与密钥")
            return
        }
        val registry = AIConnectorRegistry(profileProvider = profileStore::load)
        val connector = runCatching {
            registry.resolve(registry.connection(profile))
        }.getOrElse {
            toast("AI Connector 初始化失败：${it.javaClass.simpleName}")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("交给 AI 执行")
            .setMessage("将施工卡说明发送给当前 Context 绑定的 AI。返回内容会保存为“待审查”结果；此操作不会自动修改本地文件、提交代码或触发 GitHub Verify。")
            .setNegativeButton("取消", null)
            .setPositiveButton("开始执行") { _, _ ->
                lateinit var executionJob: Job
                executionJob = lifecycleScope.launch(start = CoroutineStart.LAZY) {
                    try {
                        toast("AI 执行已开始")
                        withContext(Dispatchers.IO) {
                            DevelopmentTaskExecutionService().execute(
                                contextId = contextId,
                                taskId = taskId,
                                store = tasks,
                                connector = connector
                            )
                        }
                        render()
                        showDetails(taskId)
                        toast("AI 已返回结果，施工卡进入待审查")
                    } catch (cancelled: CancellationException) {
                        // The service records CANCELLED when the Activity lifecycle cancels this work.
                        throw cancelled
                    } catch (error: Exception) {
                        render()
                        showDetails(taskId)
                        // Provider exceptions may contain URLs or request details; keep the UI message sanitized.
                        toast("AI 执行未完成：${error.javaClass.simpleName}")
                    } finally {
                        if (activeExecutionJobs[taskId] === executionJob) {
                            activeExecutionJobs.remove(taskId)
                        }
                        // Cancellation also refreshes the visible status from the persisted Task snapshot.
                        render()
                    }
                }
                activeExecutionJobs[taskId] = executionJob
                executionJob.start()
            }.show()
    }

    private fun changeStatus(task: DevelopmentTask, label: String) {
        val target = when (label) {
            "设为待执行", "重新设为待执行" -> DevelopmentTaskStatus.READY
            "标记开始执行" -> DevelopmentTaskStatus.RUNNING
            "提交审查" -> DevelopmentTaskStatus.REVIEW_REQUIRED
            "审查通过并完成" -> DevelopmentTaskStatus.COMPLETED
            "标记失败" -> DevelopmentTaskStatus.FAILED
            else -> DevelopmentTaskStatus.CANCELLED
        }
        if (task.status == DevelopmentTaskStatus.FAILED && target == DevelopmentTaskStatus.READY) {
            AlertDialog.Builder(this)
                .setTitle("重新排队失败任务")
                .setMessage(
                    "上次失败原因：\n" + task.errorMessage.ifBlank { "未记录具体原因" } +
                        "\n\n已执行尝试：" + task.attempt + " 次。确认后会清除当前失败信息并将任务放回 READY 队列；不会自动再次调用 AI。"
                )
                .setNegativeButton("暂不重试", null)
                .setPositiveButton("确认重新排队") { _, _ ->
                    try {
                        tasks.transition(contextId, task.id, DevelopmentTaskStatus.READY, System.currentTimeMillis())
                        render()
                        toast("任务已重新排队，需手动启动 AI 执行")
                    } catch (e: Exception) {
                        toast("重新排队失败：" + (e.message ?: "未知错误"))
                    }
                }
                .show()
            return
        }
        val result = EditText(this).apply {
            hint = when (target) {
                DevelopmentTaskStatus.REVIEW_REQUIRED -> "执行结果 / 审查摘要（必填）"
                DevelopmentTaskStatus.COMPLETED -> "审查结论 / 结果摘要（必填；不代表 GitHub Verify 通过）"
                DevelopmentTaskStatus.FAILED -> "失败原因（必填）"
                else -> "说明（可留空）"
            }
        }
        val requiresInput = target == DevelopmentTaskStatus.REVIEW_REQUIRED ||
            target == DevelopmentTaskStatus.COMPLETED || target == DevelopmentTaskStatus.FAILED
        val save = {
            try {
                tasks.transition(
                    contextId, task.id, target, System.currentTimeMillis(),
                    resultSummary = if (target == DevelopmentTaskStatus.REVIEW_REQUIRED ||
                        target == DevelopmentTaskStatus.COMPLETED) result.text.toString().trim() else "",
                    errorMessage = if (target == DevelopmentTaskStatus.FAILED) result.text.toString().trim() else ""
                )
                render(); toast("Task 状态已更新")
            } catch (e: Exception) { toast("状态更新失败：${e.message ?: "未知错误"}") }
        }
        if (requiresInput) AlertDialog.Builder(this).setTitle(label).setView(result)
            .setNegativeButton("取消", null).setPositiveButton("保存") { _, _ -> save() }.show()
        else AlertDialog.Builder(this).setTitle(label).setMessage("确认将 Task 状态更新为 $target？")
            .setNegativeButton("取消", null).setPositiveButton("确认") { _, _ -> save() }.show()
    }

    private fun chooseNode(taskId: String) {
        val nodes = tree.allNodes(contextId)
        if (nodes.isEmpty()) { toast("请先在软件开发结构中创建节点"); return }
        val labels = nodes.map { "${it.name} · ${it.type} · ${it.code}" }
        AlertDialog.Builder(this).setTitle("关联结构节点（可多次添加）")
            .setItems(labels.toTypedArray()) { _, which ->
                val node = nodes[which]
                val existing = links.forTask(contextId, taskId).firstOrNull { it.nodeId == node.id && it.relationType == "implements" }
                if (existing != null) toast("该关联已存在")
                else try {
                    links.save(DevelopmentTaskLink(
                        id = UUID.randomUUID().toString(), contextId = contextId,
                        taskId = taskId, nodeId = node.id
                    ))
                    toast("结构节点已关联")
                    showDetails(taskId)
                    render()
                } catch (e: Exception) { toast("关联失败：${e.message ?: "未知错误"}") }
            }.setNegativeButton("取消", null).show()
    }

    private fun chooseLinkToRemove(taskId: String) {
        val currentLinks = links.forTask(contextId, taskId)
        if (currentLinks.isEmpty()) { toast("此施工卡没有结构节点关联"); return }
        val nodeNames = tree.allNodes(contextId).associateBy { it.id }
        val labels = currentLinks.map { link ->
            "${nodeNames[link.nodeId]?.name ?: link.nodeId} · ${link.relationType}"
        }
        AlertDialog.Builder(this).setTitle("选择要解除的结构节点关联")
            .setItems(labels.toTypedArray()) { _, which ->
                val link = currentLinks[which]
                AlertDialog.Builder(this).setTitle("解除关联")
                    .setMessage("只会删除施工卡与结构节点之间的关联，不会删除施工卡、结构节点或验证记录。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("解除") { _, _ ->
                        try {
                            links.delete(contextId, link.id)
                            toast("结构节点关联已解除")
                            render()
                            showDetails(taskId)
                        } catch (e: Exception) {
                            toast("解除关联失败：${e.message ?: "未知错误"}")
                        }
                    }.show()
            }.setNegativeButton("取消", null).show()
    }

    private fun confirmDelete(taskId: String) {
        AlertDialog.Builder(this).setTitle("删除施工卡")
            .setMessage("将删除该 Task、关联链接及其本地生命周期事件；已保存的 GitHub 验证记录保留，不会删除远端 Workflow。")
            .setNegativeButton("取消", null).setPositiveButton("删除") { _, _ ->
                try { tasks.delete(contextId, taskId); render(); toast("施工卡已删除") }
                catch (e: Exception) { toast("删除失败：${e.message ?: "未知错误"}") }
            }.show()
    }

    private fun formatTime(value: Long): String =
        if (value <= 0L) "未知" else DateFormat.getDateTimeInstance().format(Date(value))

    private fun params() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { bottomMargin = dp(6) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_CONTEXT_ID = "aps.context.id"
        const val EXTRA_NODE_ID = "aps.development.node.id"
    }
}
