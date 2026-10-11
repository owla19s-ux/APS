package com.owla19s.aps.ai

import com.owla19s.aps.connection.Connection

data class AIConnection(
    override val id: String,
    val name: String,
    val avatar: String? = null
) : Connection {
    override val type: Connection.Type = Connection.Type.AI

    init {
        require(id.isNotBlank()) { "AI Connection ID 不能为空" }
        require(name.isNotBlank()) { "AI Connection 名称不能为空" }
    }
}
