package com.owla19s.aps.ai

import com.owla19s.aps.RuntimeDiagnostics
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Adapter for the Google Gemini generateContent and streamGenerateContent REST endpoints. */
class GeminiConnector(
    private val profile: AIProfile,
    private val client: OkHttpClient = defaultClient()
) : AIModelCatalogConnector {
    private val activeCall = AtomicReference<Call?>(null)

    override fun cancelCurrentRequest(): Boolean = activeCall.get()?.let { call ->
        if (call.isCanceled()) return@let false
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
            val request = Request.Builder().url(endpoint("models"))
                .header("x-goog-api-key", profile.apiKey).get().build()
            val models = client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException("获取 Gemini 模型列表失败（HTTP ${response.code}）：${safeError(body)}")
                parse(body).getAsJsonArray("models")?.mapNotNull { item ->
                    runCatching {
                        val model = item.asJsonObject
                        val methods = model.getAsJsonArray("supportedGenerationMethods")
                        if (methods != null && methods.none { it.asString == "generateContent" }) null
                        else model.get("name")?.asString?.removePrefix("models/")
                    }.getOrNull()
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
        val contents = JsonArray()
        (request.history + AIMessage(AIMessage.Role.USER, request.userText)).forEach { message ->
            contents.add(JsonObject().apply {
                addProperty("role", if (message.role == AIMessage.Role.ASSISTANT) "model" else "user")
                add("parts", JsonArray().apply { add(JsonObject().apply { addProperty("text", message.content) }) })
            })
        }
        val payload = JsonObject().apply { add("contents", contents) }
        val model = (request.modelId?.takeIf(String::isNotBlank) ?: profile.model).removePrefix("models/")
        val action = if (stream) "streamGenerateContent?alt=sse" else "generateContent"
        val builder = Request.Builder().url(endpoint("models/${encodePathSegment(model)}:$action"))
            .header("x-goog-api-key", profile.apiKey)
            .header("content-type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
        if (stream) builder.header("accept", "text/event-stream")
        val call = client.newCall(builder.build())
        check(activeCall.compareAndSet(null, call)) { "已有 AI 请求正在执行" }
        val started = System.nanoTime()
        val event = if (stream) "api.chat.stream" else "api.chat.request"
        RuntimeDiagnostics.record(event, "started")
        return try {
            val result = call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Gemini API 请求失败（HTTP ${response.code}）：${safeError(response.body?.string().orEmpty())}")
                }
                val body = response.body ?: throw IOException("Gemini API 响应没有响应体")
                if (stream) {
                    val source = body.source()
                    val output = StringBuilder()
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data.isEmpty()) continue
                        val delta = extractText(parse(data))
                        if (delta.isNotEmpty()) { output.append(delta); onDelta(delta) }
                    }
                    if (output.isEmpty()) throw IOException("Gemini 流式响应中没有可用文本")
                    AIResponse(output.toString())
                } else {
                    val text = extractText(parse(body.string()))
                    if (text.isBlank()) throw IOException("Gemini 响应中没有可用文本")
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

    private fun extractText(root: JsonObject): String =
        root.getAsJsonArray("candidates")?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("content")?.getAsJsonArray("parts")
            ?.mapNotNull { part -> runCatching { part.asJsonObject.get("text")?.asString }.getOrNull() }
            ?.joinToString("") { it }.orEmpty()

    private fun endpoint(path: String): String {
        val base = profile.normalizedBaseUrl
        require(base.startsWith("https://") || base.startsWith("http://")) { "API 地址必须以 http:// 或 https:// 开头" }
        return "$base/$path"
    }

    private fun parse(body: String): JsonObject = runCatching { JsonParser.parseString(body).asJsonObject }
        .getOrElse { throw IOException("Gemini API 返回了无法解析的 JSON") }

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun safeError(body: String): String =
        Regex("""(?i)(bearer\s+)[^\s"}]+""").replace(body) { it.groupValues[1] + "[redacted]" }
            .replace(Regex("""(?i)(x-goog-api-key["=: ]+)[^\s",}]+"""), "$1[redacted]")
            .replace(Regex("""(?i)(key["=: ]+)[^\s",}]+"""), "$1[redacted]")
            .take(240)

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).build()
    }
}
