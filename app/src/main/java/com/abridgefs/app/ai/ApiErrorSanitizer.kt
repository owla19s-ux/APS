package com.aps.ai

/**
 * Sanitizes provider-supplied error bodies before they are shown to users or wrapped in exceptions.
 * Request bodies and successful responses must never be passed here or written to diagnostics.
 */
internal object ApiErrorSanitizer {
    private val bearerPattern = Regex("""(?i)(bearer\s+)[^\s,"'}]+""")
    private val credentialPattern = Regex(
        """(?i)(["']?(?:x-api-key|x-goog-api-key|api.?key|key|authorization|access_token|refresh_token|token|secret|password|client_secret)["']?\s*[:=]\s*["']?)[^"'\s&,}]+"""
    )
    private val urlUserInfoPattern = Regex("""(?i)(https?://)[^/@\s]+@""")
    private val urlQueryPattern = Regex("""https?://[^\s?"'<>]+\?[^\s"'<>]*""")

    fun sanitize(body: String): String {
        if (body.isBlank()) return "服务未提供错误详情"
        val bearerRedacted = bearerPattern.replace(body) { it.groupValues[1] + "[redacted]" }
        val credentialsRedacted = credentialPattern.replace(bearerRedacted) {
            it.groupValues[1] + "[redacted]"
        }
        val userInfoRedacted = urlUserInfoPattern.replace(credentialsRedacted) {
            it.groupValues[1] + "[credentials redacted]@"
        }
        return urlQueryPattern.replace(userInfoRedacted, "[URL with query redacted]")
            .replace(Regex("""[\r\n\t]+"""), " ")
            .take(240)
    }
}
