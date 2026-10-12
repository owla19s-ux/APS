package com.owla19s.aps.development

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class DevelopmentTaskLinkStoreTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun links_persist_deduplicate_and_remain_context_scoped() {
        val prefs = context.getSharedPreferences("aps_development_task_links", Context.MODE_PRIVATE)
        val treePrefs = context.getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("links").commit()
        treePrefs.edit().remove("nodes").remove("relations").commit()
        try {
            val tree = DevelopmentTreeStore(context)
            tree.saveNode(DevelopmentNode("node-a", "ctx-a", "A", "ui", "Node A"))
            tree.saveNode(DevelopmentNode("node-b", "ctx-a", "B", "function", "Node B"))
            tree.saveNode(DevelopmentNode("node-c", "ctx-b", "C", "ui", "Node C"))
            val store = DevelopmentTaskLinkStore(context)
            store.save(DevelopmentTaskLink("l1", "ctx-a", "task-a", "node-a"))
            store.save(DevelopmentTaskLink("l2", "ctx-a", "task-a", "node-b"))
            store.save(DevelopmentTaskLink("l3", "ctx-a", "task-b", "node-a"))
            store.save(DevelopmentTaskLink("l4", "ctx-a", "task-a", "node-a"))
            assertEquals(2, store.forTask("ctx-a", "task-a").size)
            assertEquals(2, store.forNode("ctx-a", "node-a").size)
            assertEquals(3, DevelopmentTaskLinkStore(context).all("ctx-a").size)
            assertEquals(0, store.all("ctx-b").size)
            store.deleteForTask("ctx-a", "task-a")
            assertEquals(listOf("task-b"), store.all("ctx-a").map { it.taskId })
        } finally {
            prefs.edit().remove("links").remove("links.corrupt_backup").commit()
            treePrefs.edit().remove("nodes").remove("relations").remove("nodes.corrupt_backup").remove("relations.corrupt_backup").commit()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun link_cannot_reference_node_from_another_context() {
        val prefs = context.getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("nodes").remove("relations").commit()
        try {
            DevelopmentTreeStore(context).saveNode(
                DevelopmentNode("foreign-node", "ctx-other", "X", "ui", "Foreign")
            )
            DevelopmentTaskLinkStore(context).save(
                DevelopmentTaskLink("link", "ctx-a", "task-a", "foreign-node")
            )
        } finally {
            prefs.edit().remove("nodes").remove("relations").commit()
        }
    }
}
