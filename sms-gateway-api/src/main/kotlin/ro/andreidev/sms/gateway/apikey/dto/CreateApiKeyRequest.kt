package ro.andreidev.sms.gateway.apikey.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateApiKeyRequest(
    @field:NotBlank(message = "name is required")
    @field:Size(max = 100, message = "name is too long")
    val name: String,
    val expiresAt: Instant? = null,
)
