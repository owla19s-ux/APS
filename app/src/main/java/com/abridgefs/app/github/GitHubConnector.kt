package com.aps.github

import com.aps.connection.Connection
import com.aps.github.api.FileWriteRequest
import com.aps.github.api.GitHubApi
import com.aps.github.api.WorkflowDispatchRequest
import com.google.gson.JsonObject
import retrofit2.Response

data class GitHubWritePolicy(
    val allowFileWrites: Boolean = false,
    val allowWorkflowDispatch: Boolean = false
) {
    fun requireFileWrite() = check(allowFileWrites) { "GitHub 文件修改权限未授权" }
    fun requireWorkflowDispatch() = check(allowWorkflowDispatch) { "GitHub 工作流触发权限未授权" }
}

class GitHubConnector(
    private val api: GitHubApi,
    val connection: GitHubConnection,
    private val writePolicy: GitHubWritePolicy = GitHubWritePolicy()
) {
    private val address: GitHubAddress
        get() = connection.address

    init {
        require(connection.type == Connection.Type.GITHUB)
    }

    suspend fun repositories(perPage: Int = 30): Response<List<JsonObject>> =
        api.repositories(perPage)

    suspend fun repository(): Response<JsonObject> =
        api.repository(address.owner, address.repo)

    suspend fun branch(name: String = requireBranch()): Response<JsonObject> =
        api.branch(address.owner, address.repo, name)

    suspend fun commit(ref: String = requireBranch()): Response<JsonObject> =
        api.commit(address.owner, address.repo, ref)

    suspend fun file(path: String, ref: String? = address.branch): Response<JsonObject> =
        api.file(address.owner, address.repo, path, ref)

    suspend fun branches(): Response<List<JsonObject>> =
        api.branches(address.owner, address.repo)

    suspend fun workflowRuns(
        branch: String? = address.branch,
        headSha: String? = null,
        perPage: Int = 30
    ): Response<JsonObject> =
        api.workflowRuns(address.owner, address.repo, branch, headSha, perPage)

    suspend fun workflowRun(runId: Long): Response<JsonObject> =
        api.workflowRun(address.owner, address.repo, runId)

    suspend fun workflowArtifacts(runId: Long): Response<JsonObject> =
        api.workflowArtifacts(address.owner, address.repo, runId)

    suspend fun currentUser(): Response<JsonObject> =
        api.currentUser()

    suspend fun writeFile(
        path: String,
        request: FileWriteRequest
    ): Response<JsonObject> {
        writePolicy.requireFileWrite()
        return api.updateFile(address.owner, address.repo, path, request)
    }

    suspend fun dispatchWorkflow(
        workflowId: String,
        request: WorkflowDispatchRequest
    ): Response<Unit> {
        writePolicy.requireWorkflowDispatch()
        return api.dispatchWorkflow(address.owner, address.repo, workflowId, request)
    }

    private fun requireBranch(): String =
        address.branch?.takeIf { it.isNotBlank() }
            ?: error("GitHub Branch 未配置")
}
