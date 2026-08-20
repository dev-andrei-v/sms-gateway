package ro.andreidev.sms.gateway.sms.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SmsRequest(
    @field:NotBlank(message = "phoneNumber is required")
    @field:Pattern(
        regexp = "^\\+[1-9]\\d{7,14}$",
        message = "phoneNumber must be in E.164 format, e.g. +40712345678"
    )
    @field:Size(max = 16, message = "phoneNumber is too long")
    val phoneNumber: String,
    @field:NotBlank(message = "message is required")
    @field:Size(max = 500, message = "message is too long")
    val message: String
)
