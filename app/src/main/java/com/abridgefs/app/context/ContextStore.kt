package com.owla19s.aps.context

import android.content.Context as AndroidContext
import com.owla19s.aps.RuntimeDiagnostics
import com.owla19s.aps.github.GitHubResource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Context 的最小本地持久化边界。
 *
 * 只负责 Context 本身的保存、读取和删除。
 * Task、Conversation、Dispatcher 等不属于此 Store。
 */
class ContextStore(context: AndroidContext) {
    private val prefs = context.getSharedPreferences(PREFERENCES, AndroidContext.MODE_PRIVATE)

    fun get(id: String): Context? = load().firstOrNull { it.id == id }

    fun all(): List<Context> = load()

    fun save(context: Context) {
        write(load().filterNot { it.id == context.id } + context)
    }

    fun delete(id: String) {
        write(load().filterNot { it.id == id })
    }

    private fun load(): List<Context> {
        val raw = prefs.getString(KEY_DATA, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i)))
            }
        } catch (error: Exception) {
            val backupExists = prefs.contains(KEY_CORRUPT_BACKUP)
            if (!backupExists) prefs.edit().putString(KEY_CORRUPT_BACKUP, raw).commit()
            RuntimeDiagnostics.record(
                event = "local_store.context.read_failed",
                outcome = if (backupExists) "backup_already_exists" else "backup_preserved",
                errorType = error.javaClass.simpleName
            )
            emptyList()
        }
    }

    private fun write(contexts: List<Context>) {
        val array = JSONArray()
        contexts.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_DATA, array.toString()).apply()
    }

    private fun toJson(value: Context) = JSONObject().apply {
        put("id", value.id)
        put("name", value.name)
        put("notes", value.notes)
        value.aiConnectionId?.let { put("aiConnectionId", it) }
        put("addresses", JSONArray().apply {
            value.addresses.forEach { address ->
                put(JSONObject().apply {
                    put("id", address.id)
                    put("name", address.name)
                    put("address", address.address)
                    put("notes", address.notes)
                })
            }
        })
        put("resources", JSONArray().apply {
            value.resources.forEach { put(toResourceJson(it)) }
        })
    }

    private fun toResourceJson(value: ResourceRef) = when (value) {
        is ResourceRef.GitHub -> JSONObject().apply {
            put("type", "github")
            when (val resource = value.resource) {
                is GitHubResource.Repository -> {
                    put("resourceType", "repository")
                    put("id", resource.id)
                    put("fullName", resource.fullName)
                    resource.defaultBranch?.let { put("defaultBranch", it) }
                }
                is GitHubResource.Branch -> {
                    put("resourceType", "branch")
                    put("repositoryId", resource.repositoryId)
                    put("name", resource.name)
                }
                is GitHubResource.File -> {
                    put("resourceType", "file")
                    put("path", resource.path)
                }
                is GitHubResource.Commit -> {
                    put("resourceType", "commit")
                    put("ref", resource.ref)
                }
            }
        }
    }

    private fun fromJson(json: JSONObject): Context {
        val resources = json.optJSONArray("resources") ?: JSONArray()
        return Context(
            id = json.getString("id"),
            name = json.getString("name"),
            resources = buildList {
                for (i in 0 until resources.length()) {
                    add(fromResourceJson(resources.getJSONObject(i)))
                }
            },
            aiConnectionId = json.optString("aiConnectionId").ifBlank { null },
            notes = json.optString("notes"),
            addresses = buildList {
                val addresses = json.optJSONArray("addresses") ?: JSONArray()
                for (i in 0 until addresses.length()) {
                    val address = addresses.getJSONObject(i)
                    add(ContextAddress(
                        id = address.getString("id"),
                        name = address.getString("name"),
                        address = address.getString("address"),
                        notes = address.optString("notes")
                    ))
                }
            }
        )
    }

    private fun fromResourceJson(json: JSONObject): ResourceRef {
        require(json.getString("type") == "github") { "不支持的 Resource 类型" }
        return ResourceRef.GitHub(when (json.getString("resourceType")) {
            "repository" -> GitHubResource.Repository(
                id = json.getLong("id"),
                fullName = json.getString("fullName"),
                defaultBranch = json.optString("defaultBranch").ifBlank { null }
            )
            "branch" -> GitHubResource.Branch(
                repositoryId = json.getLong("repositoryId"),
                name = json.getString("name")
            )
            "file" -> GitHubResource.File(json.getString("path"))
            "commit" -> GitHubResource.Commit(json.getString("ref"))
            else -> error("不支持的 GitHub Resource 类型")
        })
    }

    private companion object {
        const val PREFERENCES = "aps_contexts"
        const val KEY_DATA = "data"
        const val KEY_CORRUPT_BACKUP = "data_corrupt_backup"
    }
}
