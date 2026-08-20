package ro.andreidev.sms.gateway.sms.entity

enum class MessageStatus {
    UNKNOWN,
    PENDING,
    SENT,
    DELIVERED,
    FAILED
}
