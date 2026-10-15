package com.owla19s.aps

import com.owla19s.aps.ai.ApiErrorSanitizer

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.InterruptedIOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small local diagnostic log for startup, runtime failures, and API request outcomes.
 * Event names and values must be controlled labels; never pass prompts, response bodies,
 * credentials, headers, or arbitrary server messages to this API.
 */
object RuntimeDiagnostics {
    private const val LOG_DIRECTORY = "aps-diagnostics"
    private const val RETENTION_DAYS = 7L
    private const val MAX_LOG_FILE_BYTES = 1_048_576L
    private val lock = Any()

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        record("app.start", "success")
    }

    fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous !is RuntimeCrashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(RuntimeCrashHandler(previous))
        }
    }

    fun recordApiEvent(
        event: String,
        outcome: String,
        durationMs: Long,
        error: Throwable? = null
    ) {
        val httpStatus = error?.message
            ?.let { Regex("""HTTP\s+(\d{3})""").find(it)?.groupValues?.getOrNull(1) }
        val failureCategory = classifyApiFailure(error, httpStatus)
        record(
            event = event,
            outcome = outcome,
            durationMs = durationMs,
            httpStatus = httpStatus,
            errorType = error?.javaClass?.simpleName,
            failureCategory = failureCategory
        )
    }

    internal fun shouldRotateLog(fileLength: Long): Boolean = fileLength >= MAX_LOG_FILE_BYTES

    internal fun nextRotatedLogFile(directory: File, date: String, timestampMillis: Long): File {
        val baseName = "aps-$date-$timestampMillis"
        var candidate = File(directory, "$baseName.log")
        var suffix = 1
        while (candidate.exists()) {
            candidate = File(directory, "$baseName-$suffix.log")
            suffix++
        }
        return candidate
    }

    internal fun classifyApiFailure(error: Throwable?, httpStatus: String? = null): String? {
        return when {
            error == null -> null
            httpStatus == "401" -> "authentication"
            httpStatus == "403" -> "permission_denied"
            httpStatus == "404" -> "endpoint_not_found"
            httpStatus == "429" -> "rate_limited"
            httpStatus?.toIntOrNull()?.let { it in 500..599 } == true -> "provider_server_error"
            httpStatus != null -> "http_error"
            error is SocketTimeoutException -> "timeout"
            error is UnknownHostException -> "dns_or_host"
            error is InterruptedIOException -> "timeout_or_cancelled"
            error is IllegalArgumentException -> "invalid_configuration"
            error is IOException -> "network_io"
            else -> "unexpected_error"
        }
    }

    fun readRecentLogs(context: Context, maxChars: Int = 24_000): String {
        return try {
            val directory = File(context.applicationContext.filesDir, LOG_DIRECTORY)
            val files = directory.listFiles()
                ?.filter { it.isFile && it.name.startsWith("aps-") && it.name.endsWith(".log") }
                ?.sortedBy { it.lastModified() }
                .orEmpty()
            if (files.isEmpty()) return "暂无诊断日志。"

            // Walk newest files first and enforce a total byte budget before decoding.
            // This avoids loading every rotated file into memory just to display/export the tail.
            var remainingBytes = maxChars.coerceAtLeast(0).toLong()
            val newestFirst = mutableListOf<String>()
            for (file in files.asReversed()) {
                if (remainingBytes <= 0L) break
                val budget = minOf(remainingBytes, Int.MAX_VALUE.toLong()).toInt()
                val text = runCatching { readFileTail(file, budget) }.getOrNull().orEmpty()
                if (text.isNotEmpty()) newestFirst.add(text)
                remainingBytes -= minOf(file.length(), budget.toLong())
            }
            newestFirst.asReversed().joinToString("\n").takeLast(maxChars)
                .ifBlank { "暂无诊断日志。" }
        } catch (_: Exception) {
            "读取诊断日志失败。"
        }
    }

    /** Export the retained local diagnostics to a user-selected document URI. */
    fun exportRecentLogs(context: Context, destination: android.net.Uri): Boolean {
        return try {
            val logs = readRecentLogs(
                context,
                maxChars = (RETENTION_DAYS * MAX_LOG_FILE_BYTES).toInt()
            )
            val output = context.contentResolver.openOutputStream(destination) ?: return false
            output.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(logs) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun record(
        event: String,
        outcome: String,
        durationMs: Long? = null,
        httpStatus: String? = null,
        errorType: String? = null,
        failureCategory: String? = null
    ) {
        val safeEvent = safeLabel(event)
        val safeOutcome = safeLabel(outcome)
        val line = buildString {
            append(timestamp())
            append(" event=").append(safeEvent)
            append(" outcome=").append(safeOutcome)
            append(" thread=").append(safeLabel(Thread.currentThread().name))
            durationMs?.let { append(" duration_ms=").append(it.coerceAtLeast(0)) }
            httpStatus?.takeIf { it.matches(Regex("""\d{3}""")) }
                ?.let { append(" http_status=").append(it) }
            errorType?.let { append(" error_type=").append(safeLabel(it)) }
            failureCategory?.let { append(" failure_category=").append(safeLabel(it)) }
        }
        appendLineSafely(line)
    }

    internal fun recordCrash(thread: Thread, throwable: Throwable) {
        appendLineSafely(formatCrash(thread, throwable))
    }

    internal fun formatCrash(thread: Thread, throwable: Throwable): String = buildString {
        append(timestamp())
        append(" event=app.crash outcome=uncaught")
        append(" thread=").append(safeLabel(thread.name))
        append(" error_type=").append(safeLabel(throwable.javaClass.name))
        append('\n').append(" exception_type=").append(safeLabel(throwable.javaClass.name))
        throwable.message?.let {
            append('\n').append(" exception_message=").append(safeExceptionMessage(it))
        }
        throwable.stackTrace.take(60).forEach { frame ->
            append('\n').append("  at ").append(safeLabel(frame.toString()))
        }
        var cause = throwable.cause
        var depth = 0
        while (cause != null && depth < 5) {
            append('\n').append(" caused_by_type=").append(safeLabel(cause.javaClass.name))
            cause.message?.let {
                append(" message=").append(safeExceptionMessage(it))
            }
            cause.stackTrace.take(20).forEach { frame ->
                append('\n').append("  caused_by_at ").append(safeLabel(frame.toString()))
            }
            cause = cause.cause
            depth++
        }
    }

    private fun readFileTail(file: File, maxBytes: Int): String {
        if (maxBytes <= 0 || !file.isFile) return ""
        RandomAccessFile(file, "r").use { input ->
            val length = input.length()
            val start = (length - maxBytes).coerceAtLeast(0L)
            input.seek(start)
            if (start > 0L) {
                input.seek(start - 1L)
                val previousByte = input.readByte()
                input.seek(start)
                // Skip only when the offset begins in the middle of a line.
                if (previousByte != '\n'.code.toByte()) {
                    while (input.filePointer < length && input.readByte().toInt() != '\n'.code) Unit
                }
            }
            val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
            val buffer = ByteArray(8192)
            var remaining = (maxBytes.toLong() - (input.filePointer - start))
                .coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                if (count <= 0) break
                output.write(buffer, 0, count)
                remaining -= count
            }
            return output.toString(Charsets.UTF_8.name())
        }
    }

    private fun appendLineSafely(line: String) {
        val context = appContext ?: return
        try {
            synchronized(lock) {
                val directory = File(context.filesDir, LOG_DIRECTORY)
                if (!directory.exists() && !directory.mkdirs()) return
                pruneOldLogs(directory)
                val file = File(directory, "aps-" + fileDate() + ".log")
                if (file.exists() && shouldRotateLog(file.length())) {
                    // Preserve the full segment instead of erasing earlier diagnostic evidence.
                    val rotated = nextRotatedLogFile(directory, fileDate(), System.currentTimeMillis())
                    if (!file.renameTo(rotated)) return
                }
                FileWriter(file, true).buffered().use { writer ->
                    writer.append(line)
                    writer.newLine()
                }
            }
        } catch (_: Exception) {
            // Diagnostics must never bring down the app's primary flow.
        }
    }

    private fun pruneOldLogs(directory: File) {
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS * 24 * 60 * 60 * 1000
        directory.listFiles()
            ?.filter { it.isFile && it.name.startsWith("aps-") && it.name.endsWith(".log") }
            ?.filter { it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    private fun safeExceptionMessage(value: String): String =
        ApiErrorSanitizer.sanitize(value)

    private fun safeLabel(value: String): String =
        value.replace(Regex("""[\r\n\t]"""), " ")
            .replace(Regex("""(?i)bearer\s+[^\s]+"""), "Bearer [redacted]")
            .take(180)

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())

    private fun fileDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

private class RuntimeCrashHandler(
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        RuntimeDiagnostics.recordCrash(thread, throwable)
        if (previous != null) {
            previous.uncaughtException(thread, throwable)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}
