package com.owla19s.aps.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyProfileMigrationGuidanceTest {
    @Test
    fun noLegacyDataDoesNotShowWarning() {
        assertNull(
            LegacyProfileMigrationGuidance.message(
                hasLegacyData = false,
                name = null,
                baseUrl = null,
                model = null,
                encryptedKey = null,
                keyCanBeRead = { error("Must not inspect credentials without legacy data") }
            )
        )
    }

    @Test
    fun legacyHttpEndpointExplainsHttpsRequirementWithoutReadingKey() {
        val message = LegacyProfileMigrationGuidance.message(
            hasLegacyData = true,
            name = "Old API",
            baseUrl = "http://localhost:8080/v1",
            model = "model-a",
            encryptedKey = "retained-encrypted-value",
            keyCanBeRead = { error("Must not attempt to decrypt a key for an HTTP endpoint") }
        )

        assertTrue(message.orEmpty().contains("HTTP"))
        assertTrue(message.orEmpty().contains("HTTPS"))
        assertTrue(message.orEmpty().contains("原配置仍保留"))
    }

    @Test
    fun incompleteLegacyDataExplainsRecoveryAndDoesNotReadKey() {
        val message = LegacyProfileMigrationGuidance.message(
            hasLegacyData = true,
            name = "Old API",
            baseUrl = "https://api.example.com/v1",
            model = null,
            encryptedKey = "retained-encrypted-value",
            keyCanBeRead = { error("Incomplete data should be reported first") }
        )

        assertTrue(message.orEmpty().contains("配置不完整"))
        assertTrue(message.orEmpty().contains("原数据已保留"))
    }

    @Test
    fun unreadableLegacyKeyExplainsRecoveryWithoutExposingSecret() {
        val message = LegacyProfileMigrationGuidance.message(
            hasLegacyData = true,
            name = "Old API",
            baseUrl = "https://api.example.com/v1",
            model = "model-a",
            encryptedKey = "sensitive-ciphertext",
            keyCanBeRead = { false }
        )

        assertTrue(message.orEmpty().contains("无法解密"))
        assertTrue(message.orEmpty().contains("原配置数据仍保留"))
        assertTrue(!message.orEmpty().contains("sensitive-ciphertext"))
    }

    @Test
    fun readableHttpsLegacyProfileNeedsNoWarning() {
        assertNull(
            LegacyProfileMigrationGuidance.message(
                hasLegacyData = true,
                name = "Old API",
                baseUrl = "https://api.example.com/v1",
                model = "model-a",
                encryptedKey = "encrypted",
                keyCanBeRead = { true }
            )
        )
    }
}
