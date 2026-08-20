package ro.andreidev.sms.gateway.portability.service

class InvalidPhoneNumberException(message: String) : RuntimeException(message)

class PortabilityLookupException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
