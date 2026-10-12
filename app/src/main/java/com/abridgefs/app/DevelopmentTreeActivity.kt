package com.owla19s.aps

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.owla19s.aps.context.ContextStore
import com.owla19s.aps.development.DevelopmentTreeBranch
import com.owla19s.aps.development.DevelopmentTreeStore

/** Context-scoped, read-only first view of the persisted software-development structure. */
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
            text = "当前为只读浏览。层级来自已保存的 parent_id；节点编码仅用于识别，不用于推断父子关系。"
            textSize = 13f
            setTextColor(Color.rgb(69, 88, 108))
            setPadding(0, dp(4), 0, dp(12))
        }, params())
        val branches = treeStore.tree(contextId)
        if (branches.isEmpty()) {
            content.addView(TextView(this).apply {
                text = "此 Context 还没有结构节点。后续批次将补充节点创建与编辑。"
                textSize = 15f
                setPadding(dp(12), dp(14), dp(12), dp(14))
                setBackgroundColor(Color.WHITE)
            }, params())
        } else {
            branches.forEach { addBranch(content, it, 0) }
        }
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
        if (hasChildren) row.setOnClickListener {
            if (!expanded.add(node.id)) expanded.remove(node.id)
            render()
        }
        parent.addView(row, params().apply { bottomMargin = dp(4) })
        if (hasChildren && node.id in expanded) branch.children.forEach { addBranch(parent, it, depth + 1) }
    }

    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(6) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_CONTEXT_ID = "aps.context.id"
    }
}