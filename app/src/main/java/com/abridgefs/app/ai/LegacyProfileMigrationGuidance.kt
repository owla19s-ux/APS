package com.owla19s.aps.ai

/**
 * Produces a safe, actionable explanation when a legacy API profile cannot be migrated.
 * It never includes the saved endpoint or credential in user-facing text.
 */
internal object LegacyProfileMigrationGuidance {
    fun message(
        hasLegacyData: Boolean,
        name: String?,
        baseUrl: String?,
        model: String?,
        encryptedKey: String?,
        keyCanBeRead: () -> Boolean
    ): String? {
        if (!hasLegacyData) return null
        if (name.isNullOrBlank() || baseUrl.isNullOrBlank() ||
            model.isNullOrBlank() || encryptedKey.isNullOrBlank()
        ) {
            return "检测到旧版 API 配置不完整，原数据已保留。请重新填写连接名称、HTTPS 地址、API Key 和模型后保存。"
        }

        val validationError = runCatching { ApiEndpoint.validateBaseUrl(baseUrl) }.exceptionOrNull()
        if (validationError != null) {
            return if (validationError.message.orEmpty().contains("HTTPS", ignoreCase = true)) {
                "旧版 API 地址使用了 HTTP，无法自动迁移。原配置仍保留；请为服务配置 HTTPS，然后重新填写地址并保存。"
            } else {
                "旧版 API 地址未通过安全校验，原配置仍保留。请检查地址并使用有效的 HTTPS Base URL 重新保存。"
            }
        }

        if (!keyCanBeRead()) {
            return "检测到旧版 API 密钥无法解密，原配置数据仍保留。为避免丢失信息，请重新输入 API Key 和连接信息后保存。"
        }
        return null
    }
}
