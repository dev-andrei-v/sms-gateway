package ro.andreidev.sms.gateway.portability.client

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("sms-gateway.portability")
data class PortabilityConfig(
    @field:NotBlank(message = "sms-gateway.portability.base-url must not be blank")
    @field:Pattern(
        regexp = "^https?://.+",
        message = "sms-gateway.portability.base-url must start with http:// or https://"
    )
    val baseUrl: String = "https://www.portabilitate.ro",
    val sslVerify: Boolean = true,
    val allowInsecureSslFallback: Boolean = true,
    @field:Min(1, message = "sms-gateway.portability.timeout-seconds must be at least 1")
    val timeoutSeconds: Long = 15,
)
