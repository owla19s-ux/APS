package com.owla19s.aps.local

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalConnectionStoreTest {
    @Test
    fun malformed_connection_data_is_backed_up_without_exposing_raw_value_to_logs() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("aps_local_connections", Context.MODE_PRIVATE)
        val oldData = prefs.getString("connections", null)
        val hadOldData = prefs.contains("connections")
        val oldBackup = prefs.getString("connections_corrupt_backup", null)
        val hadOldBackup = prefs.contains("connections_corrupt_backup")
        val corrupted = """[{"id":"broken","treeUri":"content://private/tree"}"""

        try {
            prefs.edit().putString("connections", corrupted)
                .remove("connections_corrupt_backup").commit()

            assertEquals(emptyList<LocalConnection>(), LocalConnectionStore(context).all())
            assertEquals(corrupted, prefs.getString("connections_corrupt_backup", null))
            assertEquals(corrupted, prefs.getString("connections", null))
        } finally {
            prefs.edit().apply {
                if (hadOldData) putString("connections", oldData) else remove("connections")
                if (hadOldBackup) putString("connections_corrupt_backup", oldBackup)
                else remove("connections_corrupt_backup")
            }.commit()
        }
    }
}
