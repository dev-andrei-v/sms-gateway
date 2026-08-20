package ro.andreidev.sms.gateway.portability.dto

import java.time.Instant

data class PortabilityLookupResponse(
    val number: String,
    val queryUrl: String,
    val sourceLanguage: String,
    val fetchedAt: Instant,
    val status: String,
    val ported: Boolean?,
    val title: String?,
    val operators: PortabilityOperators,
    val timestamps: PortabilityTimestamps,
    val numberType: String?,
)

data class PortabilityOperators(
    val current: String?,
    val initial: String?,
)

data class PortabilityTimestamps(
    val current: String?,
    val currentIso: String?,
    val infoValidOn: String?,
    val infoValidOnIso: String?,
)
