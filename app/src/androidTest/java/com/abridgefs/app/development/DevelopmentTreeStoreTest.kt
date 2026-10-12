package com.owla19s.aps.development

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DevelopmentTreeStoreTest {
    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun store(): DevelopmentTreeStore = DevelopmentTreeStore(context())
    private fun node(id: String, parent: String? = null, contextId: String = "tree-test") =
        DevelopmentNode(id, contextId, id.uppercase(), "ui", id, parent)

    @Test
    fun nodes_persist_across_store_instances() {
        val prefs = context().getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("nodes").remove("relations").commit()
        val first = store()
        first.saveNode(node("root"))
        first.saveNode(node("child", "root"))
        assertEquals(listOf(node("root"), node("child", "root")), store().allNodes("tree-test"))
        prefs.edit().remove("nodes").remove("relations").commit()
    }

    @Test
    fun moving_into_descendant_is_rejected_and_keeps_tree_unchanged() {
        val prefs = context().getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("nodes").remove("relations").commit()
        val store = store()
        store.saveNode(node("root"))
        store.saveNode(node("child", "root"))
        try {
            store.moveNode("tree-test", "root", "child")
            throw AssertionError("Expected cycle rejection")
        } catch (_: IllegalArgumentException) {
            assertNull(store.getNode("tree-test", "root")?.parentId)
        } finally {
            prefs.edit().remove("nodes").remove("relations").commit()
        }
    }

    @Test
    fun duplicate_relation_is_replaced_and_node_delete_cleans_edges() {
        val prefs = context().getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("nodes").remove("relations").commit()
        val store = store()
        store.saveNode(node("a"))
        store.saveNode(node("b"))
        store.saveRelation(DevelopmentRelation("r1", "tree-test", "a", "b", "implements"))
        store.saveRelation(DevelopmentRelation("r2", "tree-test", "a", "b", "implements"))
        assertEquals(1, store.allRelations("tree-test").size)
        assertEquals("r2", store.allRelations("tree-test").single().id)
        store.deleteNode("tree-test", "a")
        assertEquals(emptyList<DevelopmentRelation>(), store.allRelations("tree-test"))
        prefs.edit().remove("nodes").remove("relations").commit()
    }

    @Test
    fun deleting_parent_with_children_is_rejected() {
        val prefs = context().getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("nodes").remove("relations").commit()
        val store = store()
        store.saveNode(node("root"))
        store.saveNode(node("child", "root"))
        try {
            store.deleteNode("tree-test", "root")
            throw AssertionError("Expected parent deletion rejection")
        } catch (_: IllegalArgumentException) {
            assertNotNull(store.getNode("tree-test", "root"))
        } finally {
            prefs.edit().remove("nodes").remove("relations").commit()
        }
    }

    @Test
    fun relation_cannot_cross_context_boundary() {
        val prefs = context().getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("nodes").remove("relations").commit()
        val store = store()
        store.saveNode(node("a", contextId = "tree-test"))
        store.saveNode(node("b", contextId = "other-context"))
        try {
            store.saveRelation(DevelopmentRelation("r1", "tree-test", "a", "b", "implements"))
            throw AssertionError("Expected cross-context relation rejection")
        } catch (_: IllegalArgumentException) {
            assertEquals(emptyList<DevelopmentRelation>(), store.allRelations("tree-test"))
        } finally {
            prefs.edit().remove("nodes").remove("relations").commit()
        }
    }

    @Test
    fun tree_projection_and_child_queries_preserve_sort_order_and_hierarchy() {
        val prefs = context().getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("nodes").remove("relations").commit()
        val store = store()
        store.saveNode(node("root"))
        store.saveNode(node("b-child", "root").copy(sortOrder = 2))
        store.saveNode(node("a-child", "root").copy(sortOrder = 1))
        store.saveNode(node("grandchild", "a-child"))
        assertEquals(listOf("root"), store.roots("tree-test").map { it.id })
        assertEquals(listOf("a-child", "b-child"), store.childrenOf("tree-test", "root").map { it.id })
        val rootBranch = store.tree("tree-test").single()
        assertEquals("root", rootBranch.node.id)
        assertEquals(listOf("a-child", "b-child"), rootBranch.children.map { it.node.id })
        assertEquals(listOf("grandchild"), rootBranch.children.first().children.map { it.node.id })
        prefs.edit().remove("nodes").remove("relations").commit()
    }

    @Test
    fun relation_direction_queries_are_context_scoped() {
        val prefs = context().getSharedPreferences("aps_development_tree", Context.MODE_PRIVATE)
        prefs.edit().remove("nodes").remove("relations").commit()
        val store = store()
        store.saveNode(node("a"))
        store.saveNode(node("b"))
        store.saveNode(node("c"))
        store.saveRelation(DevelopmentRelation("r1", "tree-test", "a", "b", "implements"))
        store.saveRelation(DevelopmentRelation("r2", "tree-test", "c", "a", "depends_on"))
        assertEquals(listOf("r1"), store.relationsFrom("tree-test", "a").map { it.id })
        assertEquals(listOf("r2"), store.relationsTo("tree-test", "a").map { it.id })
        prefs.edit().remove("nodes").remove("relations").commit()
    }
}
