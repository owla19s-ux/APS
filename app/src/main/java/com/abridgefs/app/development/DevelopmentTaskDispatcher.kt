package com.owla19s.aps.development

/**
 * Deterministic local queue selection for READY tasks.
 *
 * This only selects eligible work; it does not start tasks, invoke an AI, or claim
 * that a GitHub workflow has run. Execution remains an explicit lifecycle action.
 */
object DevelopmentTaskDispatcher {
    fun readyQueue(
        tasks: List<DevelopmentTask>,
        contextId: String,
        duty: DevelopmentTaskDuty? = null
    ): List<DevelopmentTask> {
        require(contextId.isNotBlank()) { "Context ID 不能为空" }
        return tasks.asSequence()
            .filter { it.contextId == contextId }
            .filter { it.status == DevelopmentTaskStatus.READY }
            .filter { duty == null || it.duty == duty }
            .sortedWith(compareBy<DevelopmentTask> { it.createdAtMillis }.thenBy { it.id })
            .toList()
    }

    fun nextReady(
        tasks: List<DevelopmentTask>,
        contextId: String,
        duty: DevelopmentTaskDuty? = null
    ): DevelopmentTask? = readyQueue(tasks, contextId, duty).firstOrNull()
}
