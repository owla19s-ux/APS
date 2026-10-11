package com.owla19s.aps

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** A compact, expandable global settings hub. Unimplemented capabilities are labelled honestly. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): ScrollView {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }
        root.addView(TextView(this).apply {
            text = "设置"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "全局连接、权限、验证和系统信息"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(16))
        })
        addSection("连接与服务", "管理 AI/API、GitHub 与本地文件连接。") { panel ->
            panel.addView(actionButton("AI / API 连接") {
                startActivity(Intent(this, AISettingsActivity::class.java))
            })
            panel.addView(actionButton("GitHub 连接入口") {
                startActivity(Intent(this, ConversationActivity::class.java))
                Toast.makeText(this, "请在对话页打开“GitHub 只读”入口", Toast.LENGTH_LONG).show()
            })
            panel.addView(actionButton("本地文件入口") {
                startActivity(Intent(this, ConversationActivity::class.java))
                Toast.makeText(this, "请在对话页打开“本地文件”入口", Toast.LENGTH_LONG).show()
            })
        }
        addSection("权限", "Android 系统权限与连接授权分开管理。") { panel ->
            panel.addView(infoText("本地目录授权由系统文件选择器授予，可在对话页重新选择或撤销授权。"))
            panel.addView(infoText("GitHub 访问令牌用于连接验证，并加密保存在本机。"))
            panel.addView(infoText("独立的统一权限管理界面尚未完成。"))
        }
        addSection("验证", "显示目前可实际触发的验证入口。") { panel ->
            panel.addView(actionButton("测试 AI/API 连接") {
                startActivity(Intent(this, AISettingsActivity::class.java))
            })
            panel.addView(infoText("GitHub 验证和本地文件读写检查目前通过各自连接入口执行。"))
            panel.addView(infoText("统一 Verify / Evidence 工作台尚未完成。"))
        }
        addSection("系统", "诊断与运行信息。") { panel ->
            panel.addView(actionButton("查看运行日志") { showLogs() })
            panel.addView(infoText("当前日志保存在应用私有目录；共享存储目录尚未接入。"))
        }
        return ScrollView(this).apply { addView(root) }
    }

    private fun addSection(
        title: String,
        summary: String,
        buildContent: (LinearLayout) -> Unit
    ) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = getDrawable(android.R.drawable.dialog_holo_light_frame)
        }
        val header = Button(this).apply {
            text = "$title  ›"
            contentDescription = "展开$title"
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        panel.addView(infoText(summary))
        buildContent(panel)
        header.setOnClickListener {
            val expanded = panel.visibility != View.VISIBLE
            panel.visibility = if (expanded) View.VISIBLE else View.GONE
            header.text = "$title  ${if (expanded) "⌄" else "›"}"
            header.contentDescription = if (expanded) "收起$title" else "展开$title"
        }
        container.addView(header, matchParams())
        container.addView(panel)
        root.addView(container, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun infoText(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setPadding(dp(4), dp(8), dp(4), dp(8))
    }

    private fun showLogs() {
        val logs = RuntimeDiagnostics.readRecentLogs(this)
        val view = TextView(this).apply {
            text = logs
            textIsSelectable = true
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val scroll = ScrollView(this).apply { addView(view) }
        android.app.AlertDialog.Builder(this)
            .setTitle("运行日志（最近记录）")
            .setView(scroll)
            .setNeutralButton("复制") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("APS 运行日志", logs))
                Toast.makeText(this, "日志已复制", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun matchParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
