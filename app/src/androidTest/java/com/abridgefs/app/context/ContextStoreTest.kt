package com.owla19s.aps.context

import androidx.test.platform.app.InstrumentationRegistry
import com.owla19s.aps.github.GitHubResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ContextStoreTest {
    @Test
    fun context_persists_resources_and_can_be_deleted() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val firstStore = ContextStore(appContext)
        firstStore.delete("ctx-store-test")

        val repository = GitHubResource.Repository(
            id = 177,
            fullName = "owla19s-ux/APS",
            defaultBranch = "main"
        )
        val context = Context("ctx-store-test", "APS")
            .withResource(ResourceRef.GitHub(repository))

        firstStore.save(context)

        val secondStore = ContextStore(appContext)
        val restored = secondStore.get("ctx-store-test")

        assertNotNull(restored)
        assertEquals(context, restored)

        secondStore.delete("ctx-store-test")
        assertEquals(null, secondStore.get("ctx-store-test"))
    }

    @Test
    fun unreadable_context_data_is_backed_up_before_empty_recovery() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = appContext.getSharedPreferences("aps_contexts", android.content.Context.MODE_PRIVATE)
        val oldData = prefs.getString("data", null)
        val hadOldData = prefs.contains("data")
        val oldBackup = prefs.getString("data_corrupt_backup", null)
        val hadOldBackup = prefs.contains("data_corrupt_backup")
        val corrupted = """{"broken":"""

        try {
            prefs.edit().putString("data", corrupted).remove("data_corrupt_backup").commit()

            assertEquals(emptyList<Context>(), ContextStore(appContext).all())
            assertEquals(corrupted, prefs.getString("data_corrupt_backup", null))
            assertEquals(corrupted, prefs.getString("data", null))
        } finally {
            prefs.edit().apply {
                if (hadOldData) putString("data", oldData) else remove("data")
                if (hadOldBackup) putString("data_corrupt_backup", oldBackup) else remove("data_corrupt_backup")
            }.commit()
        }
    }

    @Test
    fun context_persists_ai_connection_binding() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val firstStore = ContextStore(appContext)
        firstStore.delete("ctx-ai-store-test")

        val context = Context("ctx-ai-store-test", "APS")
            .withAIConnection("ai:test")

        firstStore.save(context)

        val secondStore = ContextStore(appContext)
        assertEquals(context, secondStore.get("ctx-ai-store-test"))

        secondStore.delete("ctx-ai-store-test")
    }

    @Test
    fun saving_same_id_replaces_existing_context() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ContextStore(appContext)
        store.delete("ctx-store-replace-test")

        store.save(Context("ctx-store-replace-test", "First"))
        store.save(Context("ctx-store-replace-test", "Second"))

        assertEquals(
            listOf(Context("ctx-store-replace-test", "Second")),
            store.all()
        )

        store.delete("ctx-store-replace-test")
    }
}
