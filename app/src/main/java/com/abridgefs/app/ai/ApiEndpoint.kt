package com.aps.ai

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Builds API endpoint URLs without string-appending paths after query strings.
 * Credentials belong in protocol headers, not in the Base URL.
 */
internal object ApiEndpoint {
    /** Validates a Base URL before it can be persisted as profile metadata. */
    fun validateBaseUrl(baseUrl: String): HttpUrl {
        val base = baseUrl.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("API 地址无效，请填写完整的 HTTPS Base URL")
        require(base.isHttps) {
            "为保护 API Key，API Base URL 必须使用 HTTPS；请为自托管服务配置 HTTPS 后再连接"
        }
        require(base.username.isEmpty() && base.password.isEmpty()) {
            "API Base URL 不能包含用户名或密码，请将凭据放入 API Key / 认证字段"
        }
        require(base.encodedFragment == null) {
            "API Base URL 不能包含片段标识（#）"
        }
        return base
    }

    fun build(baseUrl: String, path: String, ensureV1: Boolean = false): HttpUrl {
        val base = validateBaseUrl(baseUrl)
        val builder = base.newBuilder()
        val pathSegments = base.encodedPathSegments.toMutableList()
        while (pathSegments.size > 1 && pathSegments.lastOrNull() == "") {
            builder.removePathSegment(pathSegments.lastIndex)
            pathSegments.removeAt(pathSegments.lastIndex)
        }
        if (ensureV1 && pathSegments.lastOrNull() != "v1") {
            builder.addPathSegment("v1")
        }
        return builder.addPathSegments(path).build()
    }
}
