package com.owla19s.aps.development

import org.junit.Assert.assertEquals
import org.junit.Test

class DevelopmentTaskLinkTest {
    @Test
    fun same_task_node_and_relation_share_uniqueness_key() {
        val first = DevelopmentTaskLink("link-1", "ctx-1", "task-1", "node-1", "implements")
        val second = DevelopmentTaskLink("link-2", "ctx-1", "task-1", "node-1", "implements")
        assertEquals(first.uniquenessKey, second.uniquenessKey)
    }

    @Test
    fun one_task_can_link_multiple_nodes_and_node_can_link_multiple_tasks() {
        val links = listOf(
            DevelopmentTaskLink("1", "ctx", "task-a", "node-a"),
            DevelopmentTaskLink("2", "ctx", "task-a", "node-b"),
            DevelopmentTaskLink("3", "ctx", "task-b", "node-a")
        )
        assertEquals(2, links.count { it.taskId == "task-a" })
        assertEquals(2, links.count { it.nodeId == "node-a" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun blank_task_id_is_rejected() {
        DevelopmentTaskLink("link-1", "ctx-1", " ", "node-1")
    }
}
