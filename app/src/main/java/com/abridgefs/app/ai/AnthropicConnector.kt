package com.abridgefs.app.ai

import com.abridgefs.app.RuntimeDiagnostics
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Adapter for the Anthropic Messages API; it shares APS request, cancellation and diagnostics contracts. */
class AnthropicConnector(
    private val profile: AIProfile,
    private val client: OkHttpClient = defaultClient()
) : AIModelCatalogConnector {
    private val activeCall = AtomicReference<Call?>(null)

    override fun cancelCurrentRequest(): Boolean = activeCall.get()?.let { call ->
        if (call.isCanceled) return@let false
        call.cancel()
        true
    } ?: false

    override suspend fun send(request: AIRequest): AIResponse = withContext(Dispatchers.IO) {
        executeChat(request, stream = false) {}
    }

    override suspend fun sendStreaming(request: AIRequest, onDelta: (String) -> Unit): AIResponse =
        withContext(Dispatchers.IO) { executeChat(request, stream = true, onDelta = onDelta) }

    override suspend fun testChatCompletion(): String =
        send(AIRequest(contextId = null, userText = "Reply with OK only.")).text

    override suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        try {
            val request = Request.Builder()
                .url(endpoint("models"))
                .header("x-api-key", profile.apiKey)
                .header("anthropic-version", API_VERSION)
                .get().build()
            val models = client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException("获取模型列表失败（HTTP ${response.code}）：${safeError(body)}")
                val root = parse(body)
                root.getAsJsonArray("data")?.mapNotNull { item ->
                    runCatching { item.asJsonObject.get("id")?.asString }.getOrNull()
                }.orEmpty().filter { it.isNotBlank() }.distinct().sorted()
            }
            RuntimeDiagnostics.recordApiEvent("api.models.list", "success", (System.nanoTime()-started)/1_000_000)
            models
        } catch (error: Exception) {
            RuntimeDiagnostics.recordApiEvent("api.models.list", "failed", (System.nanoTime()-started)/1_000_000, error)
            throw error
        }
    }

    private fun executeChat(request: AIRequest, stream: Boolean, onDelta: (String) -> Unit): AIResponse {
        val messages = JsonArray()
        (request.history + AIMessage(AIMessage.Role.USER, request.userText)).forEach { message ->
            messages.add(JsonObject().apply {
                addProperty("role", if (message.role == AIMessage.Role.ASSISTANT) "assistant" else "user")
                addProperty("content", message.content)
            })
        }
        val payload = JsonObject().apply {
            addProperty("model", request.modelId?.takeIf(String::isNotBlank) ?: profile.model)
            addProperty("max_tokens", DEFAULT_MAX_TOKENS)
            add("messages", messages)
            if (stream) addProperty("stream", true)
        }
        val builder = Request.Builder()
            .url(endpoint("messages"))
            .header("x-api-key", profile.apiKey)
            .header("anthropic-version", API_VERSION)
            .header("content-type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
        if (stream) builder.header("accept", "text/event-stream")
        val call = client.newCall(builder.build())
        check(activeCall.compareAndSet(null, call)) { "已有 AI 请求正在执行" }
        val started = System.nanoTime()
        val event = if (stream) "api.chat.stream" else "api.chat.request"
        RuntimeDiagnostics.record(event, "started")
        try {
            val result = call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Anthropic API 请求失败（HTTP ${response.code}）：${safeError(response.body?.string().orEmpty())}")
                }
                val body = response.body ?: throw IOException("Anthropic API 响应没有响应体")
                if (stream) {
                    val source = body.source()
                    val output = StringBuilder()
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data.isEmpty()) continue
                        val delta = runCatching {
                            val root = parse(data)
                            if (root.get("type")?.asString == "content_block_delta")
                                root.getAsJsonObject("delta")?.get("text")?.asString.orEmpty()
                            else ""
                        }.getOrDefault("")
                        if (delta.isNotEmpty()) { output.append(delta); onDelta(delta) }
                    }
                    if (output.isEmpty()) throw IOException("Anthropic 流式响应中没有可用文本")
                    AIResponse(output.toString())
                } else {
                    val root = parse(body.string())
                    val text = root.getAsJsonArray("content")?.mapNotNull { block ->
                        runCatching {
                            block.asJsonObject.takeIf { it.get("type")?.asString == "text" }
                                ?.get("text")?.asString
                        }.getOrNull()
                    }?.joinToString("") { it }.orEmpty()
                    if (text.isBlank()) throw IOException("Anthropic 响应中没有可用文本")
                    AIResponse(text)
                }
            }
            RuntimeDiagnostics.recordApiEvent(event, "success", (System.nanoTime()-started)/1_000_000)
            result
        } catch (error: Exception) {
            RuntimeDiagnostics.recordApiEvent(event, "failed", (System.nanoTime()-started)/1_000_000, error)
            throw error
        } finally {
            activeCall.compareAndSet(call, null)
        }
    }

    private fun endpoint(path: String): String {
        val base = profile.normalizedBaseUrl
        require(base.startsWith("https://") || base.startsWith("http://")) { "API 地址必须以 http:// 或 https:// 开头" }
        return if (base.endsWith("/v1")) "$base/$path" else "$base/v1/$path"
    }

    private fun parse(body: String): JsonObject = runCatching { JsonParser.parseString(body).asJsonObject }
        .getOrElse { throw IOException("Anthropic API 返回了无法解析的 JSON") }

    private fun safeError(body: String): String =
        Regex("""(?i)(bearer\s+)[^\s"}]+""").replace(body) { it.groupValues[1] + "[redacted]" }
            .replace(Regex("""(?i)(x-api-key["=: ]+)[^\s",}]+"""), "$1[redacted]").take(240)

    companion object {
        private const val API_VERSION = "2023-06-01"
        private const val DEFAULT_MAX_TOKENS = 2048
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).build()
    }
}
