package com.owla19s.aps.local

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalErrorMessageTest {
    @Test
    fun preservesExactKnownAppAuthoredMessage() {
        val error = IOException("文件在编辑期间已发生变化；为避免覆盖新内容，已取消本次保存")

        assertEquals(
            "文件在编辑期间已发生变化；为避免覆盖新内容，已取消本次保存",
            LocalErrorMessage.safe(error)
        )
    }

    @Test
    fun hidesUnknownProviderMessageAndPath() {
        val providerError = IOException("Permission denied: /private/provider/secret.txt?token=abc")

        val displayed = LocalErrorMessage.safe(providerError)

        assertFalse(displayed.contains("/private/provider/secret.txt"))
        assertFalse(displayed.contains("token=abc"))
        assertFalse(displayed.contains("Permission denied"))
        assertEquals("文档提供方操作失败（IOException）。请检查目录授权后重试。", displayed)
    }

    @Test
    fun doesNotAllowPrefixOrSuffixVariantsOfKnownMessages() {
        val providerError = IOException(
            "文件在编辑期间已发生变化；为避免覆盖新内容，已取消本次保存 / private path"
        )

        assertFalse(LocalErrorMessage.safe(providerError).contains("/ private path"))
        assertEquals("文档提供方操作失败（IOException）。请检查目录授权后重试。", LocalErrorMessage.safe(providerError))
    }

    @Test
    fun handlesMissingExceptionMessage() {
        assertEquals(
            "文档提供方操作失败（IllegalStateException）。请检查目录授权后重试。",
            LocalErrorMessage.safe(IllegalStateException())
        )
    }
}
