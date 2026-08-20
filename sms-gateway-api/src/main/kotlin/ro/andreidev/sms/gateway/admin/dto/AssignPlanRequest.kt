package ro.andreidev.sms.gateway.admin.dto

import jakarta.validation.constraints.NotBlank

data class AssignPlanRequest(
    @field:NotBlank(message = "planCode is required")
    val planCode: String,
)
