package ro.andreidev.sms.gateway.apikey.dto

import java.time.Instant

data class ApiKeyResponse(
    val id: Long,
    val name: String,
    val prefix: String,
    val enabled: Boolean,
    val expiresAt: Instant?,
    val lastUsedAt: Instant?,
    val createdAt: Instant,
)
