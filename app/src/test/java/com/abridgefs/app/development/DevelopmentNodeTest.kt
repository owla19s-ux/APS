package com.aps.development

import org.junit.Assert.assertEquals
import org.junit.Test

class DevelopmentNodeTest {
    @Test
    fun identity_and_parent_are_independent_fields() {
        val node = DevelopmentNode("node-1", "ctx-1", "UI-001", "ui", "Home", "parent-1")
        assertEquals("node-1", node.id)
        assertEquals("parent-1", node.parentId)
        assertEquals("UI-001", node.code)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blank_identity_is_rejected() {
        DevelopmentNode("", "ctx-1", "UI-001", "ui", "Home")
    }

    @Test(expected = IllegalArgumentException::class)
    fun self_parent_is_rejected() {
        DevelopmentNode("node-1", "ctx-1", "UI-001", "ui", "Home", "node-1")
    }
}
