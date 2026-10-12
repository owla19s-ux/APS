package com.owla19s.aps.development

/** Read-only projection used by structure-tree UI; it does not duplicate persisted nodes. */
data class DevelopmentTreeBranch(
    val node: DevelopmentNode,
    val children: List<DevelopmentTreeBranch>
)
