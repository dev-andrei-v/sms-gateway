package ro.andreidev.sms.gateway.sms.client

interface SmsGatewayClient {
    fun sendMessage(phoneNumber: String, message: String): SmsSendResponse

    data class SmsSendResponse(
        val state: SmsProcessingState = SmsProcessingState.UNKNOWN,
        val providerId: String? = null,
    )

    enum class SmsProcessingState {
        PENDING,
        SENT,
        DELIVERED,
        FAILED,
        UNKNOWN;
    }
}
