package ro.andreidev.sms.gateway.sms.dto

sealed class SmsSendResult {
    data class Success(val message: MessageResponse) : SmsSendResult()
    data class QuotaExceeded(val reason: String) : SmsSendResult()
    data class Failure(val reason: String) : SmsSendResult()
}
