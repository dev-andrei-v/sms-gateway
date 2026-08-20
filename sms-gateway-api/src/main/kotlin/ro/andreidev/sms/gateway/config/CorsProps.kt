package ro.andreidev.sms.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sms-gateway.cors")
data class CorsProps(
    private val allowedOrigins: String = "*",
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"),
    val allowedHeaders: List<String> = listOf("Authorization", "Content-Type", "Accept", "Origin", "X-API-Key"),
    val exposedHeaders: List<String> = emptyList(),
) {
    fun getAllowedOriginsList(): List<String> {
        return if (allowedOrigins.isBlank()) {
            emptyList()
        } else {
            allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
