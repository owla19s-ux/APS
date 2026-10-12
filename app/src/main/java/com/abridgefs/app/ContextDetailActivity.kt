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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.owla19s.aps.context.Context as APSContext
import com.owla19s.aps.context.ContextAddress
import com.owla19s.aps.context.ContextStore
import java.util.UUID

/** Context detail and address management. Address records locate resources; they do not sync them. */
class ContextDetailActivity : AppCompatActivity() {
    private lateinit var store: ContextStore
    private lateinit var contextId: String
    private lateinit var root: LinearLayout
    private lateinit var heading: TextView
    private lateinit var notesView: TextView
    private lateinit var aiConnectionView: TextView
    private lateinit var addressesList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contextId = intent.getStringExtra(EXTRA_CONTEXT_ID).orEmpty()
        store = ContextStore(this)
        if (contextId.isBlank() || store.get(contextId) == null) {
            Toast.makeText(this, "Context 不存在或已删除", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContentView(buildUi())
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) refresh()
    }

    private fun buildUi(): ScrollView {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
            setBackgroundColor(Color.rgb(243, 246, 250))
        }
        heading = TextView(this).apply {
            textSize = 26f
            setTextColor(Color.rgb(28, 43, 61))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(heading)
        root.addView(Button(this).apply {
            text = "编辑 Context 名称与备注"
            setOnClickListener { editContext() }
        }, params())
        notesView = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.rgb(69, 88, 108))
            setPadding(dp(12), dp(10), dp(12), dp(12))
            background = roundedSurface(Color.WHITE, 12)
        }
        root.addView(notesView)
        root.addView(TextView(this).apply {
            text = "默认 AI 连接"
            textSize = 18f
            setTextColor(Color.rgb(28, 43, 61))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(16), 0, dp(4))
        })
        aiConnectionView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(69, 88, 108))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedSurface(Color.WHITE, 12)
        }
        root.addView(aiConnectionView, params())
        root.addView(Button(this).apply {
            text = "设置 Context 默认 API 连接"
            setOnClickListener { chooseAIConnection() }
        }, params())
        val addressHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        addressHeader.addView(TextView(this).apply {
            text = "项目地址"
            textSize = 20f
            setTextColor(Color.rgb(28, 43, 61))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addressHeader.addView(Button(this).apply {
            text = "添加地址"
            setOnClickListener { editAddress(null) }
        })
        root.addView(addressHeader)
        root.addView(TextView(this).apply {
            text = "地址用于记录资源位置；添加地址不会自动复制、同步或修改目标资源。"
            textSize = 13f
            setPadding(0, dp(4), 0, dp(8))
        })
        addressesList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(addressesList)
        root.addView(Button(this).apply {
            text = "软件开发结构"
            setOnClickListener {
                startActivity(Intent(this@ContextDetailActivity, DevelopmentTreeActivity::class.java)
                    .putExtra(DevelopmentTreeActivity.EXTRA_CONTEXT_ID, contextId))
            }
        }, params())
        root.addView(Button(this).apply {
            text = "从此 Context 开始对话"
            setOnClickListener {
                startActivity(Intent(this@ContextDetailActivity, ConversationActivity::class.java)
                    .putExtra("aps.context.id", contextId))
            }
        }, params())
        root.addView(Button(this).apply {
            text = "打开本地文件"
            setOnClickListener {
                startActivity(Intent(this@ContextDetailActivity, ConversationActivity::class.java)
                    .putExtra("aps.open.local.files", true)
                    .putExtra("aps.context.id", contextId))
            }
        }, params())
        root.addView(Button(this).apply {
            text = "连接与服务"
            setOnClickListener { startActivity(Intent(this@ContextDetailActivity, SettingsActivity::class.java)) }
        }, params())
        root.addView(Button(this).apply {
            text = "返回空间"
            setOnClickListener { finish() }
        }, params())
        return ScrollView(this).apply { addView(root) }
    }

    private fun refresh() {
        val context = store.get(contextId) ?: run { finish(); return }
        heading.text = context.name
        notesView.text = context.notes.ifBlank { "暂无备注" }
        val profiles = com.owla19s.aps.ai.AIProfileStore(this)
        val selectedProfile = context.aiConnectionId?.let(profiles::load)
        aiConnectionView.text = if (selectedProfile == null) {
            val defaultProfile = profiles.load()
            if (defaultProfile == null) "未配置 API 连接；开始对话时将使用全局默认连接（如有）"
            else "使用全局默认：${defaultProfile.name} · ${defaultProfile.model}"
        } else {
            "${selectedProfile.name} · ${selectedProfile.model}"
        }
        addressesList.removeAllViews()
        if (context.addresses.isEmpty()) {
            addressesList.addView(TextView(this).apply {
                text = "尚未添加项目地址。可添加 GitHub 仓库、本地目录或其他资源位置。"
                textSize = 14f
                setPadding(0, dp(8), 0, dp(12))
            })
        }
        context.addresses.forEach { address ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = roundedSurface(Color.WHITE, 12)
            }
            card.addView(TextView(this).apply {
                text = address.name
                textSize = 17f
                setTextColor(Color.rgb(28, 43, 61))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = address.address
                textSize = 14f
                setTextIsSelectable(true)
                setPadding(0, dp(4), 0, dp(4))
            })
            if (address.notes.isNotBlank()) card.addView(TextView(this).apply {
                text = address.notes
                textSize = 13f
            })
            val actions = LinearLayout(this)
            actions.addView(Button(this).apply {
                text = "编辑"
                setOnClickListener { editAddress(address) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(Button(this).apply {
                text = "删除"
                setOnClickListener { confirmDeleteAddress(address) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(actions)
            addressesList.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
        }
    }

    private fun chooseAIConnection() {
        val profiles = com.owla19s.aps.ai.AIProfileStore(this)
        val available = profiles.loadAll()
        val current = store.get(contextId) ?: return
        val options = mutableListOf("跟随全局默认连接")
        options += available.map { "${it.name} · ${it.model}" }
        val selectedIndex = current.aiConnectionId?.let { id ->
            available.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1)
        } ?: 0
        AlertDialog.Builder(this)
            .setTitle("Context 默认 API 连接")
            .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, which ->
                val connectionId = if (which == 0) null else available[which - 1].id
                store.save(current.copy(aiConnectionId = connectionId))
                refresh()
                dialog.dismiss()
                toast(if (connectionId == null) "已设置为跟随全局默认连接" else "Context 默认连接已保存")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun editContext() {
        val current = store.get(contextId) ?: return
        val name = EditText(this).apply {
            hint = "Context 名称"
            setText(current.name)
        }
        val notes = EditText(this).apply {
            hint = "备注（可留空）"
            minLines = 3
            setText(current.notes)
            gravity = Gravity.TOP
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(name, params())
            addView(notes, params())
        }
        AlertDialog.Builder(this)
            .setTitle("编辑 Context")
            .setView(form)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newName = name.text.toString().trim()
                if (newName.isBlank()) {
                    toast("Context 名称不能为空")
                } else {
                    store.save(current.copy(name = newName, notes = notes.text.toString().trim()))
                    refresh()
                    toast("Context 已保存")
                }
            }.show()
    }

    private fun editAddress(existing: ContextAddress?) {
        val name = EditText(this).apply {
            hint = "地址名称，例如主仓库"
            setText(existing?.name.orEmpty())
        }
        val address = EditText(this).apply {
            hint = "地址，例如 owner/repository 或本地目录"
            setText(existing?.address.orEmpty())
            setSingleLine(true)
        }
        val notes = EditText(this).apply {
            hint = "备注（可留空）"
            setText(existing?.notes.orEmpty())
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(name, params())
            addView(address, params())
            addView(notes, params())
        }
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "添加项目地址" else "编辑项目地址")
            .setView(form)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val current = store.get(contextId) ?: return@setPositiveButton
                val enteredName = name.text.toString().trim()
                val enteredAddress = address.text.toString().trim()
                if (enteredName.isBlank() || enteredAddress.isBlank()) {
                    toast("地址名称和地址不能为空")
                } else {
                    val item = ContextAddress(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        name = enteredName,
                        address = enteredAddress,
                        notes = notes.text.toString().trim()
                    )
                    store.save(current.copy(
                        addresses = current.addresses.filterNot { it.id == item.id } + item
                    ))
                    refresh()
                    toast("项目地址已保存")
                }
            }.show()
    }

    private fun confirmDeleteAddress(address: ContextAddress) {
        AlertDialog.Builder(this)
            .setTitle("删除项目地址")
            .setMessage("只删除 APS 中的地址记录，不会删除目标仓库或文件。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                val current = store.get(contextId) ?: return@setPositiveButton
                store.save(current.copy(addresses = current.addresses.filterNot { it.id == address.id }))
                refresh()
            }.show()
    }

    private fun roundedSurface(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(6) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_CONTEXT_ID = "aps.context.id"
    }
}
