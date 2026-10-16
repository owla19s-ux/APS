package com.aps.context

import android.content.Context as AndroidContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Reusable Context starter configuration.
 * Templates deliberately exclude addresses, resources, and credentials: applying a template
 * creates a new Context with its own identity and only copies user-editable starter settings.
 */
data class ContextTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val notes: String = "",
    val enabledCapabilities: Set<String> = ContextCapability.defaultForNewContext
) {
    init {
        require(id.isNotBlank()) { "模板 ID 不能为空" }
        require(name.isNotBlank()) { "模板名称不能为空" }
        require(enabledCapabilities.all { it in ContextCapability.all }) { "模板包含未知功能" }
    }

    fun createContext(contextName: String): Context = Context(
        id = UUID.randomUUID().toString(),
        name = contextName.trim(),
        notes = notes,
        enabledCapabilities = enabledCapabilities.toSet()
    )
}

/** Local CRUD store for built-in/custom starter configurations. */
class ContextTemplateStore(context: AndroidContext) {
    private val prefs = context.getSharedPreferences(PREFERENCES, AndroidContext.MODE_PRIVATE)

    fun all(): List<ContextTemplate> = try {
        val raw = prefs.getString(KEY_DATA, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val capabilities = item.optJSONArray("enabledCapabilities")
                add(ContextTemplate(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    notes = item.optString("notes"),
                    enabledCapabilities = capabilities?.let { values ->
                        buildSet { for (j in 0 until values.length()) add(values.getString(j)) }
                    } ?: ContextCapability.defaultForNewContext
                ))
            }
        }.sortedBy { it.name.lowercase() }
    } catch (_: Exception) {
        emptyList()
    }

    fun get(id: String): ContextTemplate? = all().firstOrNull { it.id == id }

    fun save(template: ContextTemplate) {
        val next = all().filterNot { it.id == template.id } + template
        val array = JSONArray()
        next.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("notes", item.notes)
                put("enabledCapabilities", JSONArray().apply {
                    item.enabledCapabilities.sorted().forEach { put(it) }
                })
            })
        }
        prefs.edit().putString(KEY_DATA, array.toString()).commit()
    }

    fun delete(id: String) {
        val array = JSONArray()
        all().filterNot { it.id == id }.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("notes", item.notes)
                put("enabledCapabilities", JSONArray().apply {
                    item.enabledCapabilities.sorted().forEach { put(it) }
                })
            })
        }
        prefs.edit().putString(KEY_DATA, array.toString()).commit()
    }

    companion object {
        private const val PREFERENCES = "aps_context_templates"
        private const val KEY_DATA = "templates_json"
    }
}
