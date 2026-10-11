package com.owla19s.aps.github

import com.owla19s.aps.connection.Connection

data class GitHubConnection(
    override val id: String,
    val address: GitHubAddress
) : Connection {
    override val type: Connection.Type = Connection.Type.GITHUB

    init {
        require(id.isNotBlank()) { "Connection ID 不能为空" }
    }
}
