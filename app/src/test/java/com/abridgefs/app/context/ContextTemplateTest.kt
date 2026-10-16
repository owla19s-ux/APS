package com.aps.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextTemplateTest {
    @Test
    fun applyingTemplateCreatesIndependentContextWithSelectedCapabilities() {
        val template = ContextTemplate(
            id = "template-1",
            name = "开发模板",
            notes = "初始说明",
            enabledCapabilities = setOf(ContextCapability.CONVERSATION, ContextCapability.TASKS)
        )

        val first = template.createContext("项目 A")
        val second = template.createContext("项目 B")

        assertNotEquals(template.id, first.id)
        assertNotEquals(first.id, second.id)
        assertEquals("项目 A", first.name)
        assertEquals("初始说明", first.notes)
        assertEquals(setOf(ContextCapability.CONVERSATION, ContextCapability.TASKS), first.enabledCapabilities)
        assertTrue(first.addresses.isEmpty())
        assertTrue(first.resources.isEmpty())
    }

    @Test
    fun templateRejectsUnknownCapability() {
        try {
            ContextTemplate(name = "错误模板", enabledCapabilities = setOf("unknown"))
            throw AssertionError("Expected unknown capability to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: template configurations must use stable capability identifiers.
        }
    }
}
