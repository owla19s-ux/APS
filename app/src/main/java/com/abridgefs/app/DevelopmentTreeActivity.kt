package com.owla19s.aps

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.owla19s.aps.context.ContextStore
import com.owla19s.aps.development.DevelopmentNode
import com.owla19s.aps.development.DevelopmentRelation
import com.owla19s.aps.development.DevelopmentTreeBranch
import com.owla19s.aps.development.DevelopmentTreeStore
import java.util.UUID

/** Context-scoped structure browser and node CRUD, guarded by DevelopmentTreeStore invariants. */
class DevelopmentTreeActivity : AppCompatActivity() {
    private lateinit var contextId: String
    private lateinit var treeStore: DevelopmentTreeStore
    private lateinit var content: LinearLayout
    private val expanded = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contextId = intent.getStringExtra(EXTRA_CONTEXT_ID).orEmpty()
        if (contextId.isBlank() || ContextStore(this).get(contextId) == null) {
            Toast.makeText(this, "Context 不存在或已删除", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        treeStore = DevelopmentTreeStore(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
            setBackgroundColor(Color.rgb(243, 246, 250))
        }
        setContentView(ScrollView(this).apply { addView(content) })
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized && ::treeStore.isInitialized) render()
    }

    private fun render() {
        content.removeAllViews()
        content.addView(TextView(this).apply {
            text = "软件开发结构"
            textSize = 26f
            setTextColor(Color.rgb(28, 43, 61))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, params())
        content.addView(TextView(this).apply {
            text = "按 parent_id 展示层级。点击节点管理名称、类型、编码、备注或调整父节点。"
            textSize = 13f
            setTextColor(Color.rgb(69, 88, 108))
            setPadding(0, dp(4), 0, dp(12))
        }, params())
        content.addView(Button(this).apply {
            text = "新增顶层节点"
            setOnClickListener { editNode(null, null) }
        }, params())
        content.addView(Button(this).apply {
            text = "管理关系边"
            setOnClickListener { showRelationsDialog() }
        }, params())
        val branches = treeStore.tree(contextId)
        if (branches.isEmpty()) {
            content.addView(TextView(this).apply {
                text = "此 Context 还没有结构节点。点击“新增顶层节点”开始建立结构。"
                textSize = 15f
                setPadding(dp(12), dp(14), dp(12), dp(14))
                setBackgroundColor(Color.WHITE)
            }, params())
        } else branches.forEach { addBranch(content, it, 0) }
    }

    private fun addBranch(parent: LinearLayout, branch: DevelopmentTreeBranch, depth: Int) {
        val node = branch.node
        val hasChildren = branch.children.isNotEmpty()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12 + depth * 18), dp(10), dp(10), dp(10))
            setBackgroundColor(if (depth % 2 == 0) Color.WHITE else Color.rgb(249, 250, 252))
        }
        row.addView(TextView(this).apply {
            text = (if (hasChildren) (if (node.id in expanded) "▾  " else "▸  ") else "•  ") +
                node.name + if (hasChildren) "  (${branch.children.size})" else ""
            textSize = 16f
            setTextColor(Color.rgb(28, 43, 61))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        row.addView(TextView(this).apply {
            text = "${node.type} · ${node.code}"
            textSize = 12f
            setTextColor(Color.rgb(95, 111, 130))
            setPadding(dp(22), dp(3), 0, 0)
        })
        if (node.notes.isNotBlank()) row.addView(TextView(this).apply {
            text = node.notes
            textSize = 13f
            setTextColor(Color.rgb(69, 88, 108))
            setPadding(dp(22), dp(3), 0, 0)
        })
        row.setOnClickListener { nodeActions(node, hasChildren) }
        parent.addView(row, params().apply { bottomMargin = dp(4) })
        if (hasChildren && node.id in expanded) branch.children.forEach { addBranch(parent, it, depth + 1) }
    }

    private fun nodeActions(node: DevelopmentNode, hasChildren: Boolean) {
        val actions = mutableListOf("编辑节点", "添加子节点", if (node.id in expanded) "收起子节点" else "展开子节点", "移动节点", "删除节点", "查看关联施工卡")
        AlertDialog.Builder(this).setTitle(node.name).setItems(actions.toTypedArray()) { dialog, which ->
            when (which) {
                0 -> editNode(node, node.parentId)
                1 -> editNode(null, node.id)
                2 -> { if (!expanded.add(node.id)) expanded.remove(node.id); render() }
                3 -> chooseParent(node)
                4 -> confirmDelete(node, hasChildren)
                5 -> startActivity(Intent(this, DevelopmentTaskActivity::class.java)
                    .putExtra(DevelopmentTaskActivity.EXTRA_CONTEXT_ID, contextId)
                    .putExtra(DevelopmentTaskActivity.EXTRA_NODE_ID, node.id))
            }
            dialog.dismiss()
        }.setNegativeButton("取消", null).show()
    }

    private fun showRelationsDialog() {
        val relations = treeStore.allRelations(contextId)
        val labels = relations.map { relation ->
            "${nodeLabel(relation.sourceId)} — ${relation.relationType} → ${nodeLabel(relation.targetId)}"
        }.ifEmpty { listOf("暂无关系边；点击“新增关系边”创建") }
        AlertDialog.Builder(this)
            .setTitle("关系边管理")
            .setItems(labels.toTypedArray()) { _, which ->
                if (relations.isNotEmpty()) confirmDeleteRelation(relations[which])
            }
            .setPositiveButton("新增关系边") { _, _ -> chooseRelationSource() }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun chooseRelationSource() {
        val nodes = treeStore.allNodes(contextId)
        if (nodes.size < 2) {
            toast("至少需要两个结构节点才能建立关系边")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("选择关系源节点")
            .setItems(nodes.map { "${it.name} · ${it.type}" }.toTypedArray()) { _, which ->
                chooseRelationTarget(nodes[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun chooseRelationTarget(source: DevelopmentNode) {
        val targets = treeStore.allNodes(contextId).filter { it.id != source.id }
        AlertDialog.Builder(this)
            .setTitle("选择目标节点")
            .setItems(targets.map { "${it.name} · ${it.type}" }.toTypedArray()) { _, which ->
                editRelation(source, targets[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun editRelation(source: DevelopmentNode, target: DevelopmentNode) {
        val relationType = EditText(this).apply { hint = "关系类型（如 depends_on / implements）" }
        val notes = EditText(this).apply { hint = "备注（可留空）"; minLines = 2 }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(TextView(this@DevelopmentTreeActivity).apply {
                text = "${source.name} → ${target.name}"
                textSize = 14f
                setTextColor(Color.rgb(69, 88, 108))
            }, params())
            addView(relationType, params())
            addView(notes, params())
        }
        AlertDialog.Builder(this)
            .setTitle("新增关系边")
            .setView(form)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val type = relationType.text.toString().trim()
                if (type.isBlank()) {
                    toast("关系类型不能为空")
                    return@setPositiveButton
                }
                try {
                    treeStore.saveRelation(
                        DevelopmentRelation(
                            id = UUID.randomUUID().toString(),
                            contextId = contextId,
                            sourceId = source.id,
                            targetId = target.id,
                            relationType = type,
                            notes = notes.text.toString().trim()
                        )
                    )
                    toast("关系边已保存")
                    showRelationsDialog()
                } catch (e: Exception) {
                    toast("保存失败：${e.message ?: "未知错误"}")
                }
            }
            .show()
    }

    private fun confirmDeleteRelation(relation: DevelopmentRelation) {
        AlertDialog.Builder(this)
            .setTitle("删除关系边")
            .setMessage("${nodeLabel(relation.sourceId)} — ${relation.relationType} → ${nodeLabel(relation.targetId)}\n只删除这条关系，不删除结构节点或施工卡。")
            .setNegativeButton("取消") { _, _ -> showRelationsDialog() }
            .setPositiveButton("删除") { _, _ ->
                try {
                    treeStore.deleteRelation(contextId, relation.id)
                    toast("关系边已删除")
                    showRelationsDialog()
                } catch (e: Exception) {
                    toast("删除失败：${e.message ?: "未知错误"}")
                }
            }
            .show()
    }

    private fun nodeLabel(nodeId: String): String =
        treeStore.getNode(contextId, nodeId)?.name ?: "已删除节点"

    private fun editNode(existing: DevelopmentNode?, parentId: String?) {
        val name = EditText(this).apply { hint = "节点名称"; setText(existing?.name.orEmpty()) }
        val code = EditText(this).apply { hint = "节点编码"; setText(existing?.code.orEmpty()) }
        val type = EditText(this).apply { hint = "节点类型（UI/功能/架构/施工卡/验证等）"; setText(existing?.type.orEmpty()) }
        val notes = EditText(this).apply { hint = "备注（可留空）"; setText(existing?.notes.orEmpty()); minLines = 2 }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(name, params()); addView(code, params()); addView(type, params()); addView(notes, params())
        }
        AlertDialog.Builder(this).setTitle(if (existing == null) "新增结构节点" else "编辑结构节点")
            .setView(form).setNegativeButton("取消", null).setPositiveButton("保存") { _, _ ->
                val n = name.text.toString().trim(); val c = code.text.toString().trim(); val t = type.text.toString().trim()
                if (n.isBlank() || c.isBlank() || t.isBlank()) { toast("名称、编码和类型不能为空"); return@setPositiveButton }
                try {
                    val siblings = treeStore.allNodes(contextId).filter { it.parentId == parentId && it.id != existing?.id }
                    val node = DevelopmentNode(
                        id = existing?.id ?: UUID.randomUUID().toString(), contextId = contextId,
                        code = c, type = t, name = n, parentId = parentId,
                        sortOrder = existing?.sortOrder ?: ((siblings.maxOfOrNull { it.sortOrder } ?: -1) + 1),
                        notes = notes.text.toString().trim()
                    )
                    treeStore.saveNode(node)
                    if (parentId != null) expanded.add(parentId)
                    render(); toast("节点已保存")
                } catch (e: Exception) { toast("保存失败：${e.message ?: "未知错误"}") }
            }.show()
    }

    private fun chooseParent(node: DevelopmentNode) {
        val candidates = treeStore.allNodes(contextId).filter { it.id != node.id && !isDescendant(it.id, node.id) }
        val labels = listOf("设为顶层节点") + candidates.map { "${it.name} · ${it.type}" }
        AlertDialog.Builder(this).setTitle("移动节点：${node.name}").setItems(labels.toTypedArray()) { _, which ->
            val parentId = if (which == 0) null else candidates[which - 1].id
            try {
                treeStore.moveNode(contextId, node.id, parentId)
                parentId?.let { expanded.add(it) }
                expanded.remove(node.id)
                render(); toast("节点已移动")
            } catch (e: Exception) { toast("移动失败：${e.message ?: "未知错误"}") }
        }.setNegativeButton("取消", null).show()
    }

    private fun isDescendant(candidateId: String, ancestorId: String): Boolean {
        var current = treeStore.getNode(contextId, candidateId)
        val visited = mutableSetOf<String>()
        while (current != null && visited.add(current.id)) {
            if (current.parentId == ancestorId) return true
            current = current.parentId?.let { treeStore.getNode(contextId, it) }
        }
        return false
    }

    private fun confirmDelete(node: DevelopmentNode, hasChildren: Boolean) {
        val message = if (hasChildren) "该节点还有子节点，必须先移动或删除子节点。继续删除将被数据层拒绝。" else "删除节点会清理其关系边及施工卡关联，不会删除其他节点。"
        AlertDialog.Builder(this).setTitle("删除结构节点").setMessage(message)
            .setNegativeButton("取消", null).setPositiveButton("删除") { _, _ ->
                try { treeStore.deleteNode(contextId, node.id); expanded.remove(node.id); render(); toast("节点已删除") }
                catch (e: Exception) { toast("删除失败：${e.message ?: "未知错误"}") }
            }.show()
    }

    private fun params() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { bottomMargin = dp(6) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object { const val EXTRA_CONTEXT_ID = "aps.context.id" }
}