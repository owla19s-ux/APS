package com.owla19s.aps.github

import com.owla19s.aps.context.Context
import com.owla19s.aps.github.api.GitHubApi
import com.owla19s.aps.github.api.GitHubApiFactory

/**
 * 从 Context + GitHub Credential 装配可执行的 GitHub Connector。
 *
 * 只负责连接边界装配；认证验证、资源解析和实际 API 操作分别由现有组件负责。
 */
object GitHubConnectorFactory {
    fun create(
        context: Context,
        credential: GitHubCredential,
        writePolicy: GitHubWritePolicy = GitHubWritePolicy()
    ): GitHubConnector =
        create(
            context = context,
            api = GitHubApiFactory.create(credential),
            writePolicy = writePolicy
        )

    internal fun create(
        context: Context,
        api: GitHubApi,
        writePolicy: GitHubWritePolicy = GitHubWritePolicy()
    ): GitHubConnector {
        val connection = GitHubConnectionFactory.create(context)
        return GitHubConnector(api, connection, writePolicy)
    }
}
