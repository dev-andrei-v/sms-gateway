package ro.andreidev.sms.gateway.internal.controller

import jakarta.validation.Valid
import ro.andreidev.sms.gateway.sms.dto.OtpSmsRequest
import ro.andreidev.sms.gateway.sms.dto.SmsSendResult
import ro.andreidev.sms.gateway.sms.service.SmsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthentikController(
    private val smsService: SmsService
) {
    @PostMapping("/internal/authentik/otp")
    fun authentikOtp(@Valid @RequestBody otpSmsRequest: OtpSmsRequest): ResponseEntity<String> {
        return when (val result = smsService.sendOtpSms(otpSmsRequest)) {
            is SmsSendResult.Success -> ResponseEntity.ok("OTP SMS sent successfully")
            is SmsSendResult.QuotaExceeded -> ResponseEntity.status(429).body(result.reason)
            is SmsSendResult.Failure -> ResponseEntity.status(502).body("Failed to send OTP SMS: ${result.reason}")
        }
    }
}
