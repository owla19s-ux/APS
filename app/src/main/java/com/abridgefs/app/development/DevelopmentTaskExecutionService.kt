package com.owla19s.aps.development

import com.owla19s.aps.ai.AIConnector
import com.owla19s.aps.ai.AIRequest
import kotlinx.coroutines.CancellationException

/**
 * Explicit Task-to-AI handoff. It executes one READY task once and records the outcome locally.
 * This does not commit code or dispatch/verify a GitHub workflow.
 */
class DevelopmentTaskExecutionService {
    suspend fun execute(
        contextId: String,
        taskId: String,
        store: DevelopmentTaskStore,
        connector: AIConnector,
        nowMillis: () -> Long = System::currentTimeMillis
    ): DevelopmentTask {
        val task = store.get(contextId, taskId) ?: error("Task 不存在或不属于当前 Context")
        require(task.status == DevelopmentTaskStatus.READY) { "只有 READY Task 可以交给 AI 执行" }

        // Claim first. A concurrent attempt will fail the state transition instead of duplicating work.
        store.transition(contextId, taskId, DevelopmentTaskStatus.RUNNING, nowMillis())
        try {
            val prompt = buildString {
                appendLine("你正在 APS 中执行一张施工卡。请只处理本任务，并明确说明结果、未完成事项和风险。")
                appendLine("职责：${task.duty.displayName}")
                appendLine("任务：${task.title}")
                if (task.description.isNotBlank()) {
                    appendLine("任务说明：")
                    appendLine(task.description)
                }
                appendLine("请给出可供后续审查的执行结果。不要声称已修改本地文件、提交代码或运行 GitHub Verify，除非本次工具实际完成了这些操作。")
            }
            val response = connector.send(AIRequest(contextId = contextId, userText = prompt))
            require(response.text.isNotBlank()) { "AI 返回了空结果" }
            return store.transition(
                contextId, taskId, DevelopmentTaskStatus.REVIEW_REQUIRED, nowMillis(),
                resultSummary = response.text.trim()
            )
        } catch (cancelled: CancellationException) {
            runCatching {
                val current = store.get(contextId, taskId)
                if (current?.status == DevelopmentTaskStatus.RUNNING) {
                    store.transition(contextId, taskId, DevelopmentTaskStatus.CANCELLED, nowMillis())
                }
            }
            throw cancelled
        } catch (error: Exception) {
            runCatching {
                val current = store.get(contextId, taskId)
                if (current?.status == DevelopmentTaskStatus.RUNNING) {
                    store.transition(
                        contextId, taskId, DevelopmentTaskStatus.FAILED, nowMillis(),
                        errorMessage = "AI 执行失败：${error.javaClass.simpleName}"
                    )
                }
            }
            throw error
        }
    }
}
