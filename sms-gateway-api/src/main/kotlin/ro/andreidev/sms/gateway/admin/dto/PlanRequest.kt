package ro.andreidev.sms.gateway.admin.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ro.andreidev.sms.gateway.plan.entity.QuotaType

data class PlanRequest(
    @field:NotBlank(message = "code is required")
    @field:Size(max = 50, message = "code is too long")
    val code: String,

    @field:NotBlank(message = "name is required")
    @field:Size(max = 100, message = "name is too long")
    val name: String,

    val quotaType: QuotaType = QuotaType.UNLIMITED,

    @field:Min(value = 1, message = "quotaLimit must be at least 1")
    val quotaLimit: Int? = null,

    @field:Min(value = 0, message = "minDelaySeconds must be non-negative")
    val minDelaySeconds: Int? = null,

    @field:Min(value = 1, message = "maxRecipientsPerRequest must be at least 1")
    val maxRecipientsPerRequest: Int? = null,

    val isActive: Boolean = true,
)
