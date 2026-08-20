package ro.andreidev.sms.gateway.apikey.dto

import java.time.Instant

data class CreateApiKeyResponse(
    val id: Long,
    val name: String,
    val key: String,
    val prefix: String,
    val expiresAt: Instant?,
    val createdAt: Instant,
)
