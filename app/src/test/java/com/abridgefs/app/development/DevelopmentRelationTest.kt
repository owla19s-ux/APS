package com.owla19s.aps.development

import org.junit.Assert.assertEquals
import org.junit.Test

class DevelopmentRelationTest {
    @Test
    fun relation_key_deduplicates_same_semantic_edge() {
        val first = DevelopmentRelation("rel-1", "ctx-1", "ui-1", "fn-1", "implements")
        val second = DevelopmentRelation("rel-2", "ctx-1", "ui-1", "fn-1", "implements")
        assertEquals(first.uniquenessKey, second.uniquenessKey)
    }

    @Test(expected = IllegalArgumentException::class)
    fun self_relation_is_rejected() {
        DevelopmentRelation("rel-1", "ctx-1", "node-1", "node-1", "depends_on")
    }
}
