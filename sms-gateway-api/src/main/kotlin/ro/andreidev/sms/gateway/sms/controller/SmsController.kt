package ro.andreidev.sms.gateway.sms.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import ro.andreidev.sms.gateway.sms.dto.MessageResponse
import ro.andreidev.sms.gateway.sms.dto.OtpSmsRequest
import ro.andreidev.sms.gateway.sms.dto.SmsSendResult
import ro.andreidev.sms.gateway.sms.dto.SmsRequest
import ro.andreidev.sms.gateway.sms.service.SmsService
import ro.andreidev.sms.gateway.user.entity.User
import ro.andreidev.sms.gateway.user.filter.UserResolutionFilter.Companion.USER_ATTRIBUTE
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sms")
class SmsController(
    private val smsService: SmsService
) {
    @GetMapping
    fun getMessages(
        @PageableDefault(size = 20, sort = ["createDate"]) pageable: Pageable,
        request: HttpServletRequest
    ): ResponseEntity<Page<MessageResponse>> {
        val user = request.getAttribute(USER_ATTRIBUTE) as User
        return ResponseEntity.ok(smsService.getMessages(user, pageable))
    }

    @PostMapping
    fun sendSms(
        @Valid @RequestBody smsRequest: SmsRequest,
        request: HttpServletRequest
    ): ResponseEntity<*> {
        val user = request.getAttribute(USER_ATTRIBUTE) as User
        return mapSendResult(smsService.sendSms(smsRequest, user))
    }

    @PostMapping("/otp")
    fun sendOtpSms(
        @Valid @RequestBody otpSmsRequest: OtpSmsRequest,
        request: HttpServletRequest
    ): ResponseEntity<String> {
        val user = request.getAttribute(USER_ATTRIBUTE) as User
        return mapOtpResult(smsService.sendOtpSms(otpSmsRequest, user))
    }

    private fun mapSendResult(result: SmsSendResult): ResponseEntity<*> = when (result) {
        is SmsSendResult.Success -> ResponseEntity.ok(result.message)
        is SmsSendResult.QuotaExceeded -> ResponseEntity.status(429).body(result.reason)
        is SmsSendResult.Failure -> ResponseEntity.status(502).body(result.reason)
    }

    private fun mapOtpResult(result: SmsSendResult): ResponseEntity<String> = when (result) {
        is SmsSendResult.Success -> ResponseEntity.ok("SMS sent successfully")
        is SmsSendResult.QuotaExceeded -> ResponseEntity.status(429).body(result.reason)
        is SmsSendResult.Failure -> ResponseEntity.status(502).body(result.reason)
    }
}
