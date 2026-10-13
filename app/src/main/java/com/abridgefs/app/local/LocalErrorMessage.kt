package com.owla19s.aps.local

/**
 * Converts platform/provider failures into UI-safe messages.
 * Only exact app-authored messages are allowed through; provider text is never displayed.
 */
internal object LocalErrorMessage {
    private val safeMessages = setOf(
        "文件在编辑期间已发生变化；为避免覆盖新内容，已取消本次保存",
        "文件在保存前已发生变化；为避免覆盖新内容，已取消本次保存",
        "文件超过 2 MiB 文本读取上限",
        "文件超过 2 MiB 文本写入上限",
        "原文件超过 2 MiB 安全备份上限，已取消写入以保护现有内容",
        "写入后校验失败，文件内容与目标内容不一致",
        "当前本地连接只有读取权限，不能修改文件",
        "当前本地连接未授权读取",
        "当前只允许读取或修改可识别的文本文件",
        "不能读取目录内容为文本",
        "不能把目录当作文本文件写入",
        "无法读取原文件，已取消写入以保护现有内容",
        "无法访问本地目录；请检查目录授权是否仍有效",
        "无法打开本地文件",
        "无法打开本地文件进行写入"
    )

    fun safe(error: Throwable): String {
        val message = error.message.orEmpty()
        return safeMessages.firstOrNull { it == message }
            ?: "文档提供方操作失败（" + error.javaClass.simpleName + "）。请检查目录授权后重试。"
    }
}
