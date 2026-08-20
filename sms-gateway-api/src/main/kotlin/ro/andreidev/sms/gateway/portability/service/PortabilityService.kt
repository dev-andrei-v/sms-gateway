package ro.andreidev.sms.gateway.portability.service

import ro.andreidev.sms.gateway.portability.client.PortabilityClient
import ro.andreidev.sms.gateway.portability.dto.PortabilityLookupResponse
import org.springframework.stereotype.Service

@Service
class PortabilityService(
    private val portabilityClient: PortabilityClient,
) {
    fun lookup(phoneNumber: String): PortabilityLookupResponse =
        portabilityClient.lookup(normalizeRomanianPhoneNumber(phoneNumber), DEFAULT_LANGUAGE)

    private fun normalizeRomanianPhoneNumber(phoneNumber: String): String {
        val cleaned = phoneNumber.filter { it.isDigit() || it == '+' }
        if (cleaned.isBlank()) {
            throw InvalidPhoneNumberException("Phone number is required.")
        }

        val national = when {
            cleaned.startsWith("+4") -> cleaned.removePrefix("+4")
            cleaned.startsWith("0") -> cleaned
            else -> throw InvalidPhoneNumberException(
                "Phone number must be a Romanian number starting with 0 or +40."
            )
        }

        if (national.length != ROMANIAN_NATIONAL_LENGTH || !national.startsWith("0") || !national.all(Char::isDigit)) {
            throw InvalidPhoneNumberException(
                "Phone number must be a valid Romanian number (10 digits starting with 0)."
            )
        }

        return national
    }

    companion object {
        private const val ROMANIAN_NATIONAL_LENGTH = 10
        private const val DEFAULT_LANGUAGE = "ro"
    }
}
