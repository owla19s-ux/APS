package com.owla19s.aps.context

import android.content.Context as AndroidContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Persists Space display name, grouping directories, and Context-to-directory membership. */
data class SpaceDirectory(val id: String, val name: String)

class SpaceDirectoryStore(context: AndroidContext) {
    private val prefs = context.getSharedPreferences("aps_space_catalog", AndroidContext.MODE_PRIVATE)

    fun spaceName(): String = prefs.getString(KEY_SPACE_NAME, "APS") ?: "APS"

    fun setSpaceName(name: String) {
        require(name.isNotBlank()) { "空间名称不能为空" }
        prefs.edit().putString(KEY_SPACE_NAME, name.trim()).apply()
    }

    fun allDirectories(): List<SpaceDirectory> {
        val array = JSONArray(prefs.getString(KEY_DIRECTORIES, "[]"))
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(SpaceDirectory(item.getString("id"), item.getString("name")))
            }
        }
    }

    fun saveDirectory(directory: SpaceDirectory) {
        val updated = allDirectories().filterNot { it.id == directory.id } + directory
        val array = JSONArray()
        updated.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name)) }
        prefs.edit().putString(KEY_DIRECTORIES, array.toString()).apply()
    }

    fun createDirectory(name: String): SpaceDirectory {
        require(name.isNotBlank()) { "目录名称不能为空" }
        return SpaceDirectory(UUID.randomUUID().toString(), name.trim()).also(::saveDirectory)
    }

    fun deleteDirectory(id: String) {
        val array = JSONArray()
        allDirectories().filterNot { it.id == id }.forEach {
            array.put(JSONObject().put("id", it.id).put("name", it.name))
        }
        val memberships = memberships()
        memberships.entries.removeAll { it.value == id }
        prefs.edit()
            .putString(KEY_DIRECTORIES, array.toString())
            .putString(KEY_MEMBERSHIPS, JSONObject(memberships as Map<*, *>).toString())
            .apply()
    }

    fun directoryFor(contextId: String): String? = memberships()[contextId]

    fun setContextDirectory(contextId: String, directoryId: String?) {
        val map = memberships()
        if (directoryId == null) map.remove(contextId) else {
            require(allDirectories().any { it.id == directoryId }) { "目录不存在" }
            map[contextId] = directoryId
        }
        prefs.edit().putString(KEY_MEMBERSHIPS, JSONObject(map as Map<*, *>).toString()).apply()
    }

    fun removeContext(contextId: String) {
        val map = memberships()
        map.remove(contextId)
        prefs.edit().putString(KEY_MEMBERSHIPS, JSONObject(map as Map<*, *>).toString()).apply()
    }

    private fun memberships(): MutableMap<String, String> {
        val json = JSONObject(prefs.getString(KEY_MEMBERSHIPS, "{}"))
        val result = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.optString(key)
            if (value.isNotBlank()) result[key] = value
        }
        return result
    }

    private companion object {
        const val KEY_SPACE_NAME = "space_name"
        const val KEY_DIRECTORIES = "directories"
        const val KEY_MEMBERSHIPS = "context_directories"
    }
}
