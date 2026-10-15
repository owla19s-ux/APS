package com.aps.local

import android.content.Context
import com.aps.RuntimeDiagnostics
import org.json.JSONArray
import org.json.JSONObject

/** Persists granted tree references and per-connection read/write policy; it does not grant OS access. */
class LocalConnectionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun all(): List<LocalConnection> = load()

    fun get(id: String): LocalConnection? = load().firstOrNull { it.id == id }

    fun save(connection: LocalConnection) {
        write(load().filterNot { it.id == connection.id } + connection)
    }

    fun delete(id: String) {
        write(load().filterNot { it.id == id })
    }

    private fun load(): List<LocalConnection> {
        val raw = prefs.getString(KEY_DATA, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        LocalConnection(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            treeUri = json.getString("treeUri"),
                            canRead = json.optBoolean("canRead", true),
                            canWrite = json.optBoolean("canWrite", false)
                        )
                    )
                }
            }
        } catch (error: Exception) {
            // Preserve the original value for recovery, but never log directory URIs or raw JSON.
            if (!prefs.contains(KEY_CORRUPT_BACKUP)) {
                prefs.edit().putString(KEY_CORRUPT_BACKUP, raw).apply()
            }
            RuntimeDiagnostics.record(
                "local_store.connections.read_failed",
                "recovered",
                errorType = error.javaClass.name
            )
            emptyList()
        }
    }

    private fun write(connections: List<LocalConnection>) {
        val array = JSONArray()
        connections.forEach { value ->
            array.put(JSONObject().apply {
                put("id", value.id)
                put("name", value.name)
                put("treeUri", value.treeUri)
                put("canRead", value.canRead)
                put("canWrite", value.canWrite)
            })
        }
        prefs.edit().putString(KEY_DATA, array.toString()).apply()
    }

    private companion object {
        const val PREFERENCES = "aps_local_connections"
        const val KEY_DATA = "connections"
        const val KEY_CORRUPT_BACKUP = "connections_corrupt_backup"
    }
}
