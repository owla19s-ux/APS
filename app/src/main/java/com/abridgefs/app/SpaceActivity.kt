package com.owla19s.aps

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.owla19s.aps.context.Context as APSContext
import com.owla19s.aps.context.ContextStore
import java.util.UUID

/**
 * First usable Space entry point.
 *
 * This screen exposes only Context operations supported by ContextStore.
 * Directory nesting and Address editing will be added with their persistence model;
 * the UI does not fake those relationships.
 */
class SpaceActivity : AppCompatActivity() {
    private lateinit var store: ContextStore
    private lateinit var list: LinearLayout
    private lateinit var emptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ContextStore(this)
        setContentView(buildUi())
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized && ::list.isInitialized) refresh()
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }
        root.addView(TextView(this).apply {
            text = "空间"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "管理工作 Context，并进入对话、API 连接和本地资源操作。"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(14))
        })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        actions.addView(Button(this).apply {
            text = "新建 Context"
            setOnClickListener { promptCreateContext() }
        }, buttonParams())
        actions.addView(Button(this).apply {
            text = "普通对话"
            setOnClickListener { openConversation() }
        }, buttonParams())
        actions.addView(Button(this).apply {
            text = "API 连接设置"
            setOnClickListener {
                startActivity(Intent(this@SpaceActivity, AISettingsActivity::class.java))
            }
        }, buttonParams())
        root.addView(actions)
        root.addView(TextView(this).apply {
            text = "Context 列表"
            textSize = 20f
            setPadding(0, dp(18), 0, dp(8))
        })
        emptyState = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(8), 0, dp(12))
        }
        root.addView(emptyState)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        return ScrollView(this).apply { addView(root) }
    }

    private fun refresh() {
        list.removeAllViews()
        val contexts = store.all().sortedBy { it.name.lowercase() }
        emptyState.text = if (contexts.isEmpty()) {
            "还没有 Context。点击“新建 Context”创建第一个工作上下文。"
        } else {
            "共 \${contexts.size} 个 Context"
        }
        contexts.forEach { context ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = getDrawable(android.R.drawable.dialog_holo_light_frame)
            }
            card.addView(TextView(this).apply {
                text = context.name
                textSize = 18f
            })
            card.addView(TextView(this).apply {
                text = "资源：\${context.resources.size} · AI 连接：\${context.aiConnectionId ?: "使用默认连接"}"
                textSize = 13f
                setPadding(0, dp(4), 0, dp(8))
            })
            val buttons = LinearLayout(this)
            buttons.addView(Button(this).apply {
                text = "进入对话"
                setOnClickListener { openConversation() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            buttons.addView(Button(this).apply {
                text = "重命名"
                setOnClickListener { promptRename(context) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            buttons.addView(Button(this).apply {
                text = "删除"
                setOnClickListener { confirmDelete(context) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(buttons)
            list.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
        }
    }

    private fun promptCreateContext() {
        val field = EditText(this).apply { hint = "Context 名称" }
        AlertDialog.Builder(this)
            .setTitle("新建 Context")
            .setView(field)
            .setNegativeButton("取消", null)
            .setPositiveButton("创建") { _, _ ->
                val name = field.text.toString().trim()
                if (name.isBlank()) {
                    toast("Context 名称不能为空")
                } else {
                    store.save(APSContext(id = UUID.randomUUID().toString(), name = name))
                    refresh()
                    toast("Context 已保存")
                }
            }
            .show()
    }

    private fun promptRename(context: APSContext) {
        val field = EditText(this).apply {
            hint = "Context 名称"
            setText(context.name)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("重命名 Context")
            .setView(field)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = field.text.toString().trim()
                if (name.isBlank()) {
                    toast("Context 名称不能为空")
                } else {
                    store.save(context.copy(name = name))
                    refresh()
                    toast("已保存")
                }
            }
            .show()
    }

    private fun confirmDelete(context: APSContext) {
        AlertDialog.Builder(this)
            .setTitle("删除 Context")
            .setMessage("确定删除“\${context.name}”吗？此操作只删除 APS 中的 Context 记录，不会删除 GitHub 仓库或本地文件。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                store.delete(context.id)
                refresh()
                toast("Context 已删除")
            }
            .show()
    }

    private fun openConversation() {
        startActivity(Intent(this, ConversationActivity::class.java))
    }

    private fun buttonParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(6) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
