package ro.andreidev.sms.gateway.portability.client

import ro.andreidev.sms.gateway.portability.config.PortabilityCacheConfig.Companion.PORTABILITY_LOOKUP_CACHE
import ro.andreidev.sms.gateway.portability.dto.PortabilityLookupResponse
import ro.andreidev.sms.gateway.portability.service.InvalidPhoneNumberException
import ro.andreidev.sms.gateway.portability.service.PortabilityHtmlParser
import ro.andreidev.sms.gateway.portability.service.PortabilityLookupException
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.X509TrustManager

@Component
class PortabilityClient(
    private val portabilityConfig: PortabilityConfig,
    private val portabilityHtmlParser: PortabilityHtmlParser,
) {
    private val log = LoggerFactory.getLogger(PortabilityClient::class.java)
    private val insecureSslContext: SSLContext by lazy { buildInsecureSslContext() }
    private val insecureHostnameVerifier = HostnameVerifier { _, _ -> true }

    @Cacheable(PORTABILITY_LOOKUP_CACHE, key = "#phoneNumber + ':' + #language")
    fun lookup(phoneNumber: String, language: String): PortabilityLookupResponse {
        val normalizedPhoneNumber = normalizePhoneNumber(phoneNumber)
        val normalizedLanguage = normalizeLanguage(language)
        val path = "/$normalizedLanguage-no-$normalizedPhoneNumber"
        val html = fetchHtml(path)

        return portabilityHtmlParser.parse(
            html = html,
            phoneNumber = normalizedPhoneNumber,
            queryUrl = "${portabilityConfig.baseUrl.trimEnd('/')}$path",
            language = normalizedLanguage,
        )
    }

    private fun fetchHtml(path: String): String {
        if (!portabilityConfig.sslVerify) {
            log.warn("SSL verification is disabled for Portabilitate lookups")
            return runCatching { executeRequest(path, insecure = true) }
                .getOrElse { throw PortabilityLookupException("Failed to fetch upstream page: ${it.message}", it) }
        }

        return try {
            executeRequest(path, insecure = false)
        } catch (ex: Exception) {
            if (!portabilityConfig.allowInsecureSslFallback || !ex.containsSslException()) {
                throw PortabilityLookupException("Failed to fetch upstream page: ${ex.message}", ex)
            }

            log.warn("Retrying Portabilitate lookup without SSL verification after TLS failure: {}", ex.message)
            runCatching { executeRequest(path, insecure = true) }
                .getOrElse { throw PortabilityLookupException("Failed to fetch upstream page: ${it.message}", it) }
        }
    }

    private fun executeRequest(path: String, insecure: Boolean): String {
        val url = URI("${portabilityConfig.baseUrl.trimEnd('/')}$path").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = portabilityConfig.timeoutSeconds.toInt() * 1000
            readTimeout = portabilityConfig.timeoutSeconds.toInt() * 1000
            setRequestProperty("User-Agent", "sms-gateway-api/portability-client")
            setRequestProperty("Accept", "text/html")
        }

        if (insecure && connection is HttpsURLConnection) {
            connection.sslSocketFactory = insecureSslContext.socketFactory
            connection.hostnameVerifier = insecureHostnameVerifier
        }

        return try {
            val status = connection.responseCode
            val responseBody = readResponseBody(connection, status)

            if (status !in 200..299) {
                throw PortabilityLookupException(
                    "Upstream page responded with HTTP $status${responseBody?.let { ": ${it.take(200)}" } ?: ""}"
                )
            }

            responseBody?.takeIf { it.isNotBlank() }
                ?: throw PortabilityLookupException("Received an empty response from the upstream page.")
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponseBody(connection: HttpURLConnection, status: Int): String? {
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        if (stream == null) {
            return null
        }

        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        val normalizedPhoneNumber = phoneNumber.filter(Char::isDigit)
        if (normalizedPhoneNumber.isBlank()) {
            throw InvalidPhoneNumberException("Phone number must contain at least one digit.")
        }
        return normalizedPhoneNumber
    }

    private fun normalizeLanguage(language: String): String {
        val normalizedLanguage = language.lowercase()
        if (normalizedLanguage !in SUPPORTED_LANGUAGES) {
            throw InvalidPhoneNumberException("Unsupported language '$language'. Expected one of $SUPPORTED_LANGUAGES.")
        }
        return normalizedLanguage
    }

    private fun Throwable.containsSslException(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is SSLException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun buildInsecureSslContext(): SSLContext =
        SSLContext.getInstance("TLS").apply {
            init(
                null,
                arrayOf(
                    object : X509TrustManager {
                        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                    }
                ),
                SecureRandom(),
            )
        }

    companion object {
        private val SUPPORTED_LANGUAGES = setOf("ro", "en")
    }
}
