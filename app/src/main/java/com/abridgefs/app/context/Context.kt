package com.aps.context

import com.aps.github.GitHubResource

/**
 * APS 的具体工作上下文。
 *
 * Context 只保存工作上下文自身的稳定身份、已关联资源以及可选的当前 AI Connection。
 * Task、Conversation、Dispatcher 等能力按场景组合，不作为固定字段。
 */
data class Context(
    val id: String,
    val name: String,
    val resources: List<ResourceRef> = emptyList(),
    val aiConnectionId: String? = null,
    val notes: String = "",
    val addresses: List<ContextAddress> = emptyList(),
    val enabledCapabilities: Set<String> = ContextCapability.defaultForNewContext
) {
    init {
        require(id.isNotBlank()) { "Context ID 不能为空" }
        require(name.isNotBlank()) { "Context 名称不能为空" }
        require(aiConnectionId?.isNotBlank() != false) { "AI Connection ID 不能为空" }
    }

    fun withResource(resource: ResourceRef): Context =
        copy(resources = resources.filterNot { it.key == resource.key } + resource)

    fun withAIConnection(connectionId: String): Context =
        copy(aiConnectionId = connectionId)

    fun withCapability(capability: String, enabled: Boolean): Context = copy(
        enabledCapabilities = if (enabled) enabledCapabilities + capability else enabledCapabilities - capability
    )
}

/** Stable identifiers for independently selectable Context modules. */
object ContextCapability {
    const val CONVERSATION = "conversation"
    const val LOCAL_FILES = "local_files"
    const val GITHUB = "github"
    const val DEVELOPMENT_TREE = "development_tree"
    const val TASKS = "tasks"

    val all: List<String> = listOf(CONVERSATION, LOCAL_FILES, GITHUB, DEVELOPMENT_TREE, TASKS)
    val defaultForNewContext: Set<String> = setOf(CONVERSATION, LOCAL_FILES)
    // Context records saved before capability selection existed keep their previous entry points.
    val legacyDefault: Set<String> = all.toSet()

    fun label(capability: String): String = when (capability) {
        CONVERSATION -> "对话"
        LOCAL_FILES -> "本地文件操作"
        GITHUB -> "GitHub / Actions"
        DEVELOPMENT_TREE -> "软件开发结构"
        TASKS -> "施工卡 / Task 与 Verify"
        else -> capability
    }
}

sealed interface ResourceRef {
    val key: String

    data class GitHub(
        val resource: GitHubResource
    ) : ResourceRef {
        override val key: String = when (resource) {
            is GitHubResource.Repository -> "github:repository:" + resource.id
            is GitHubResource.Branch -> "github:branch:" + resource.repositoryId + ":" + resource.name
            is GitHubResource.File -> "github:file:" + resource.path
            is GitHubResource.Commit -> "github:commit:" + resource.ref
        }
    }
}


/** A named location reference for a Context; it does not copy or sync the resource. */
data class ContextAddress(
    val id: String,
    val name: String,
    val address: String,
    val notes: String = ""
) {
    init {
        require(id.isNotBlank()) { "Address ID 不能为空" }
        require(name.isNotBlank()) { "Address 名称不能为空" }
        require(address.isNotBlank()) { "Address 地址不能为空" }
    }
}
