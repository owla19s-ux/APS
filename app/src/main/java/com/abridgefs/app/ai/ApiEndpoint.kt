package com.owla19s.aps.ai

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Builds API endpoint URLs without string-appending paths after query strings.
 * Credentials belong in protocol headers, not in the Base URL.
 */
internal object ApiEndpoint {
    fun build(baseUrl: String, path: String, ensureV1: Boolean = false): okhttp3.HttpUrl {
        val base = baseUrl.trim().trimEnd('/').toHttpUrlOrNull()
            ?: throw IllegalArgumentException("API 地址无效，请填写完整的 http:// 或 https:// Base URL")
        require(base.encodedQuery == null) {
            "API Base URL 不能包含查询参数；请将凭证填写在 API Key 字段"
        }
        require(base.encodedFragment == null) {
            "API Base URL 不能包含片段标识（#）"
        }
        val builder = base.newBuilder()
        if (ensureV1 && base.encodedPathSegments.lastOrNull() != "v1") {
            builder.addPathSegment("v1")
        }
        return builder.addPathSegments(path).build()
    }
}
