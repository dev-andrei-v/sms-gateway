package ro.andreidev.sms.gateway.portability.service

import ro.andreidev.sms.gateway.portability.dto.PortabilityLookupResponse
import ro.andreidev.sms.gateway.portability.dto.PortabilityOperators
import ro.andreidev.sms.gateway.portability.dto.PortabilityTimestamps
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class PortabilityHtmlParser {
    private val dateTimeFormats = listOf(
        DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm:ss"),
        DateTimeFormatter.ofPattern("M/d/uuuu h:mm:ss a"),
    )
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd.MM.uuuu"),
        DateTimeFormatter.ofPattern("M/d/uuuu"),
    )
    private val infoDateRegex = Regex("""(\d{2}\.\d{2}\.\d{4}|\d{1,2}/\d{1,2}/\d{4})""")
    private val labelMap = mapOf(
        "operator curent" to "currentOperator",
        "current operator" to "currentOperator",
        "operator iniţial" to "initialOperator",
        "operator inițial" to "initialOperator",
        "initial operator" to "initialOperator",
        "data curentă" to "currentDate",
        "current date" to "currentDate",
        "tip număr" to "numberType",
        "number type" to "numberType",
    )

    fun parse(
        html: String,
        phoneNumber: String,
        queryUrl: String,
        language: String,
    ): PortabilityLookupResponse {
        val document = Jsoup.parse(html)
        val title = clean(document.selectFirst("span.ContentTitle")?.text())
            ?: throw PortabilityLookupException("Could not find the lookup result title in the upstream HTML.")
        val tableValues = extractTableValues(document)
        val currentOperator = clean(document.selectFirst("#ctl00_cphBody_lnkOperator")?.text())
            ?: tableValues["currentOperator"]
        val initialOperator = clean(document.selectFirst("#ctl00_cphBody_lnkOperatorInitial")?.text())
            ?: tableValues["initialOperator"]
        val currentDate = clean(document.selectFirst("#ctl00_cphBody_lbDataCurenta")?.text())
            ?: tableValues["currentDate"]
        val numberType = clean(document.selectFirst("#ctl00_cphBody_lbNumberType")?.text())
            ?: tableValues["numberType"]
        val infoValidOnRaw = clean(document.selectFirst("#ctl00_cphBody_lbLastUpdate")?.text())
        val infoValidOn = extractInfoValidOn(infoValidOnRaw)
        val ported = parsePortedStatus(title)
        val status = when (ported) {
            true -> "ported"
            false -> "not_ported"
            null -> "unknown"
        }

        return PortabilityLookupResponse(
            number = phoneNumber,
            queryUrl = queryUrl,
            sourceLanguage = language,
            fetchedAt = Instant.now(),
            status = status,
            ported = ported,
            title = title,
            operators = PortabilityOperators(
                current = currentOperator,
                initial = initialOperator,
            ),
            timestamps = PortabilityTimestamps(
                current = currentDate,
                currentIso = parseDateTime(currentDate),
                infoValidOn = infoValidOn,
                infoValidOnIso = parseDate(infoValidOn),
            ),
            numberType = numberType,
        )
    }

    private fun extractTableValues(document: Document): Map<String, String> {
        val table = document.selectFirst("table.warning-message") ?: return emptyMap()
        val values = mutableMapOf<String, String>()

        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size < 2) {
                return@forEach
            }

            val label = clean(cells[0].text()) ?: return@forEach
            val value = clean(cells[1].text()) ?: return@forEach
            val normalizedLabel = label.lowercase().removeSuffix(":")
            val key = labelMap[normalizedLabel] ?: return@forEach
            values[key] = value
        }

        return values
    }

    private fun parsePortedStatus(title: String): Boolean? {
        val normalizedTitle = title.lowercase()
        return when {
            "nu este portat" in normalizedTitle || "is not ported" in normalizedTitle -> false
            "este portat" in normalizedTitle || "is ported" in normalizedTitle -> true
            else -> null
        }
    }

    private fun extractInfoValidOn(value: String?): String? =
        value?.let { infoDateRegex.find(it)?.value ?: it }

    private fun parseDateTime(value: String?): String? =
        value?.let { raw ->
            dateTimeFormats.firstNotNullOfOrNull { formatter ->
                runCatching { LocalDateTime.parse(raw, formatter).toString() }.getOrNull()
            }
        }

    private fun parseDate(value: String?): String? =
        value?.let { raw ->
            dateFormats.firstNotNullOfOrNull { formatter ->
                runCatching { LocalDate.parse(raw, formatter).toString() }.getOrNull()
            }
        }

    private fun clean(value: String?): String? =
        value
            ?.replace("\\s+".toRegex(), " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}
