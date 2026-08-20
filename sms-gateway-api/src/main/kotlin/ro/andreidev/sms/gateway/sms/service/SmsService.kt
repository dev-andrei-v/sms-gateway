package ro.andreidev.sms.gateway.sms.service

import ro.andreidev.sms.gateway.sms.client.SmsGatewayClient
import ro.andreidev.sms.gateway.sms.client.SmsGatewayClient.SmsProcessingState
import org.slf4j.LoggerFactory
import ro.andreidev.sms.gateway.sms.dto.MessageResponse
import ro.andreidev.sms.gateway.sms.dto.OtpSmsRequest
import ro.andreidev.sms.gateway.sms.dto.SmsSendResult
import ro.andreidev.sms.gateway.sms.dto.SmsRequest
import ro.andreidev.sms.gateway.sms.entity.Message
import ro.andreidev.sms.gateway.sms.entity.MessageStatus
import ro.andreidev.sms.gateway.sms.repository.MessageRepository
import ro.andreidev.sms.gateway.sms.service.QuotaService.QuotaCheckResult
import ro.andreidev.sms.gateway.sms.ws.SmsStatusNotifier
import ro.andreidev.sms.gateway.user.entity.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SmsService(
    private val messageRepository: MessageRepository,
    private val smsGatewayClient: SmsGatewayClient,
    private val quotaService: QuotaService,
    private val smsStatusNotifier: SmsStatusNotifier,
    @Value("\${sms-gateway.otp-template:Your one-time authentication code is: {code}}")
    private val otpTemplate: String,
) {
    private val log = LoggerFactory.getLogger(SmsService::class.java)
    fun updateMessageStatus(providerId: String, state: String): Boolean {
        val message = messageRepository.findByProviderId(providerId) ?: return false
        val newStatus = mapStatusEvent(state)
        log.info("Message status update: providerId={} {} -> {}", providerId, message.status, newStatus)
        message.status = newStatus
        val savedMessage = messageRepository.save(message)
        smsStatusNotifier.publish(savedMessage)
        return true
    }

    fun getMessages(user: User, pageable: Pageable): Page<MessageResponse> =
        messageRepository.findByUser(user, pageable).map(MessageResponse::from)

    fun sendSms(smsRequest: SmsRequest, user: User): SmsSendResult {
        val quotaResult = checkQuota(user) ?: return sendAndPersist(smsRequest.phoneNumber, smsRequest.message, user)
        return quotaResult
    }

    fun sendOtpSms(otpSmsRequest: OtpSmsRequest, user: User? = null): SmsSendResult {
        val message = otpTemplate.replace("{code}", otpSmsRequest.otpCode)
        if (user != null) {
            val quotaResult = checkQuota(user) ?: return sendAndPersist(otpSmsRequest.phoneNumber, message, user)
            return quotaResult
        }
        return sendAndPersist(otpSmsRequest.phoneNumber, message, null)
    }

    private fun checkQuota(user: User): SmsSendResult.QuotaExceeded? {
        return when (val check = quotaService.check(user)) {
            is QuotaCheckResult.Allowed -> null
            is QuotaCheckResult.NoPlan -> SmsSendResult.QuotaExceeded("No plan assigned to user")
            is QuotaCheckResult.PlanInactive -> SmsSendResult.QuotaExceeded("User plan is inactive")
            is QuotaCheckResult.RateLimited -> SmsSendResult.QuotaExceeded(
                "Rate limited — retry after ${check.waitSeconds}s"
            )
            is QuotaCheckResult.Exceeded -> SmsSendResult.QuotaExceeded(
                "Quota exceeded: ${check.used}/${check.limit} (${check.quotaType})"
            )
        }
    }

    private fun sendAndPersist(phoneNumber: String, content: String, user: User?): SmsSendResult {
        val response = smsGatewayClient.sendMessage(phoneNumber, content)

        val savedMessage = messageRepository.save(
            Message(
                content = content,
                phoneNumber = phoneNumber,
                providerId = response.providerId,
                status = mapToMessageStatus(response.state),
                user = user
            )
        )

        return if (response.state == SmsProcessingState.PENDING) {
            SmsSendResult.Success(MessageResponse.from(savedMessage))
        } else {
            SmsSendResult.Failure("Gateway returned state: ${response.state}")
        }
    }

    private fun mapStatusEvent(event: String): MessageStatus = when (event) {
        "sms:sent" -> MessageStatus.SENT
        "sms:delivered" -> MessageStatus.DELIVERED
        "sms:failed" -> MessageStatus.FAILED
        else -> MessageStatus.UNKNOWN
    }

    private fun mapToMessageStatus(state: SmsProcessingState): MessageStatus {
        return when (state) {
            SmsProcessingState.PENDING -> MessageStatus.PENDING
            SmsProcessingState.SENT -> MessageStatus.SENT
            SmsProcessingState.DELIVERED -> MessageStatus.DELIVERED
            SmsProcessingState.FAILED -> MessageStatus.FAILED
            else -> MessageStatus.UNKNOWN
        }
    }
}
