package com.owla19s.aps

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.owla19s.aps.context.Context as APSContext
import com.owla19s.aps.context.ContextStore
import com.owla19s.aps.context.SpaceDirectory
import com.owla19s.aps.context.SpaceDirectoryStore
import java.util.UUID

/** Space entry point for Context CRUD and directory grouping. */
class SpaceActivity : AppCompatActivity() {
    private lateinit var store: ContextStore
    private lateinit var directoryStore: SpaceDirectoryStore
    private lateinit var list: LinearLayout
    private lateinit var emptyState: TextView
    private lateinit var directoryList: LinearLayout
    private lateinit var spaceTitle: TextView
    private var selectedDirectoryFilter: String = ALL_DIRECTORIES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ContextStore(this)
        directoryStore = SpaceDirectoryStore(this)
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
            setPadding(dp(18), dp(20), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 247, 251))
        }
        val heading = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        spaceTitle = TextView(this).apply {
            textSize = 30f
            setTextColor(Color.rgb(25, 34, 52))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        heading.addView(spaceTitle)
        heading.addView(Button(this).apply {
            text = "改名"
            setOnClickListener { promptRenameSpace() }
        })
        root.addView(heading)
        root.addView(TextView(this).apply {
            text = "空间内通过目录分组管理 Context。选择 Context 后可继续进入对话和连接操作。"
            textSize = 14f
            setTextColor(Color.rgb(99, 110, 130))
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(0, dp(6), 0, dp(18))
        })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val primaryActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        primaryActions.addView(actionButton("＋  新建 Context") { promptCreateContext() },
            LinearLayout.LayoutParams(0, dp(54), 1.25f).apply { marginEnd = dp(8) })
        primaryActions.addView(actionButton("＋  新建目录") { promptCreateDirectory() },
            LinearLayout.LayoutParams(0, dp(54), 1f))
        actions.addView(primaryActions, buttonParams())
        val secondaryActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        secondaryActions.addView(actionButton("普通对话") { openConversation() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
        secondaryActions.addView(actionButton("设置") {
            startActivity(Intent(this@SpaceActivity, SettingsActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(secondaryActions, buttonParams())
        root.addView(actions)
        val directoryHeading = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        directoryHeading.addView(TextView(this).apply {
            text = "目录分组"
            textSize = 20f
            setTextColor(Color.rgb(25, 34, 52))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        directoryHeading.addView(Button(this).apply {
            text = "管理目录"
            setOnClickListener { showDirectoryManagement() }
        })
        root.addView(directoryHeading)
        val directoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        directoryList = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        directoryScroll.addView(directoryList)
        root.addView(directoryScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(TextView(this).apply {
            text = "工作上下文"
            textSize = 22f
            setTextColor(Color.rgb(25, 34, 52))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            setPadding(0, dp(22), 0, dp(8))
        })
        emptyState = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(99, 110, 130))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 14)
        }
        root.addView(emptyState)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        return ScrollView(this).apply { addView(root) }
    }

    private fun refresh() {
        spaceTitle.text = directoryStore.spaceName()
        renderDirectories()
        list.removeAllViews()
        val all = store.all().sortedBy { it.name.lowercase() }
        val visible = when (selectedDirectoryFilter) {
            ALL_DIRECTORIES -> all
            UNGROUPED -> all.filter { directoryStore.directoryFor(it.id) == null }
            else -> all.filter { directoryStore.directoryFor(it.id) == selectedDirectoryFilter }
        }
        emptyState.text = when {
            visible.isEmpty() && selectedDirectoryFilter == ALL_DIRECTORIES ->
                "还没有 Context。点击“新建 Context”创建第一个工作上下文。"
            visible.isEmpty() && selectedDirectoryFilter == UNGROUPED ->
                "未分组目录为空。可以创建 Context，或将已有 Context 移入此处。"
            visible.isEmpty() -> "此目录暂无 Context。"
            else -> "显示 ${visible.size} / ${all.size} 个 Context"
        }
        visible.forEach { context ->
            val directoryName = directoryStore.directoryFor(context.id)
                ?.let { id -> directoryStore.allDirectories().firstOrNull { it.id == id }?.name }
                ?: "未分组"
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(12))
                background = rounded(Color.WHITE, 18)
                elevation = dp(2).toFloat()
            }
            card.addView(TextView(this).apply {
                text = context.name
                textSize = 19f
                setTextColor(Color.rgb(25, 34, 52))
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            })
            card.addView(TextView(this).apply {
                text = "目录：$directoryName · 资源：${context.resources.size} · AI：${context.aiConnectionId ?: "默认连接"}"
                textSize = 13f
                setPadding(0, dp(4), 0, dp(8))
            })
            val buttons = LinearLayout(this)
            buttons.addView(actionButton("详情") { openContextDetail(context) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            buttons.addView(actionButton("重命名") { promptRename(context) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            buttons.addView(actionButton("移动") { promptMoveContext(context) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            buttons.addView(actionButton("删除") { confirmDelete(context) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(buttons)
            list.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
        }
    }

    private fun renderDirectories() {
        directoryList.removeAllViews()
        addDirectoryFilter("全部", ALL_DIRECTORIES)
        addDirectoryFilter("未分组", UNGROUPED)
        directoryStore.allDirectories().forEach { directory ->
            directoryList.addView(Button(this).apply {
                text = (if (selectedDirectoryFilter == directory.id) "● " else "") + directory.name
                setOnClickListener {
                    selectedDirectoryFilter = directory.id
                    refresh()
                }
                setOnLongClickListener {
                    showDirectoryActions(directory)
                    true
                }
            })
        }
    }

    private fun addDirectoryFilter(label: String, id: String) {
        directoryList.addView(Button(this).apply {
            text = (if (selectedDirectoryFilter == id) "● " else "") + label
            setOnClickListener {
                selectedDirectoryFilter = id
                refresh()
            }
        })
    }

    private fun promptRenameSpace() {
        val field = EditText(this).apply {
            hint = "空间名称"
            setText(directoryStore.spaceName())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("修改空间名称")
            .setView(field)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = field.text.toString().trim()
                if (name.isBlank()) toast("空间名称不能为空") else {
                    directoryStore.setSpaceName(name)
                    refresh()
                }
            }.show()
    }

    private fun promptCreateDirectory() {
        val field = EditText(this).apply { hint = "目录名称" }
        AlertDialog.Builder(this)
            .setTitle("新建目录")
            .setView(field)
            .setNegativeButton("取消", null)
            .setPositiveButton("创建") { _, _ ->
                val name = field.text.toString().trim()
                if (name.isBlank()) {
                    toast("目录名称不能为空")
                } else if (directoryStore.allDirectories().any { it.name.equals(name, true) }) {
                    toast("同名目录已存在")
                } else {
                    val directory = directoryStore.createDirectory(name)
                    selectedDirectoryFilter = directory.id
                    refresh()
                    toast("目录已保存")
                }
            }.show()
    }

    private fun showDirectoryManagement() {
        val directories = directoryStore.allDirectories()
        val labels = directories.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("目录管理")
            .setItems(labels) { _, index -> showDirectoryActions(directories[index]) }
            .setNegativeButton("关闭", null)
            .setPositiveButton("新建") { _, _ -> promptCreateDirectory() }
            .show()
    }

    private fun showDirectoryActions(directory: SpaceDirectory) {
        AlertDialog.Builder(this)
            .setTitle(directory.name)
            .setItems(arrayOf("重命名", "删除目录")) { _, which ->
                if (which == 0) promptRenameDirectory(directory) else confirmDeleteDirectory(directory)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun promptRenameDirectory(directory: SpaceDirectory) {
        val field = EditText(this).apply {
            hint = "目录名称"
            setText(directory.name)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("重命名目录")
            .setView(field)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = field.text.toString().trim()
                if (name.isBlank()) toast("目录名称不能为空") else {
                    directoryStore.saveDirectory(directory.copy(name = name))
                    refresh()
                }
            }.show()
    }

    private fun confirmDeleteDirectory(directory: SpaceDirectory) {
        AlertDialog.Builder(this)
            .setTitle("删除目录")
            .setMessage("删除目录不会删除其中的 Context；这些 Context 将改为未分组。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                directoryStore.deleteDirectory(directory.id)
                if (selectedDirectoryFilter == directory.id) selectedDirectoryFilter = UNGROUPED
                refresh()
                toast("目录已删除")
            }.show()
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
                    val context = APSContext(id = UUID.randomUUID().toString(), name = name)
                    store.save(context)
                    val targetDirectory = selectedDirectoryFilter.takeIf {
                        it != ALL_DIRECTORIES && it != UNGROUPED
                    }
                    directoryStore.setContextDirectory(context.id, targetDirectory)
                    refresh()
                    toast("Context 已保存")
                }
            }.show()
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
                if (name.isBlank()) toast("Context 名称不能为空") else {
                    store.save(context.copy(name = name))
                    refresh()
                    toast("已保存")
                }
            }.show()
    }

    private fun promptMoveContext(context: APSContext) {
        val directories = directoryStore.allDirectories()
        val options = listOf("未分组") + directories.map { it.name }
        AlertDialog.Builder(this)
            .setTitle("移动：${context.name}")
            .setItems(options.toTypedArray()) { _, index ->
                val target = if (index == 0) null else directories[index - 1].id
                directoryStore.setContextDirectory(context.id, target)
                selectedDirectoryFilter = target ?: UNGROUPED
                refresh()
                toast("Context 已移动")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDelete(context: APSContext) {
        AlertDialog.Builder(this)
            .setTitle("删除 Context")
            .setMessage("确定删除“${context.name}”吗？此操作只删除 APS 中的 Context 记录，不会删除 GitHub 仓库或本地文件。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                store.delete(context.id)
                directoryStore.removeContext(context.id)
                refresh()
                toast("Context 已删除")
            }.show()
    }

    private fun openConversation() {
        startActivity(Intent(this, ConversationActivity::class.java))
    }

    private fun openContextDetail(context: APSContext) {
        startActivity(Intent(this, ContextDetailActivity::class.java).apply {
            putExtra(ContextDetailActivity.EXTRA_CONTEXT_ID, context.id)
        })
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.rgb(39, 53, 77))
        background = rounded(Color.rgb(232, 237, 247), 14)
        elevation = dp(1).toFloat()
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun buttonParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(6) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private companion object {
        const val ALL_DIRECTORIES = "__all__"
        const val UNGROUPED = "__ungrouped__"
    }
}
