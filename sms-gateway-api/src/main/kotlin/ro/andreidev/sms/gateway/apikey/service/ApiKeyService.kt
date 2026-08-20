package ro.andreidev.sms.gateway.apikey.service

import ro.andreidev.sms.gateway.apikey.entity.ApiKey
import ro.andreidev.sms.gateway.apikey.repository.ApiKeyRepository
import ro.andreidev.sms.gateway.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun create(user: User, name: String, expiresAt: Instant?): Pair<ApiKey, String> {
        val rawKey = generateRawKey()
        val apiKey = apiKeyRepository.save(
            ApiKey(
                keyHash = sha256(rawKey),
                keyPrefix = rawKey.take(12),
                name = name,
                user = user,
                expiresAt = expiresAt,
            )
        )
        return apiKey to rawKey
    }

    fun listByUser(user: User): List<ApiKey> =
        apiKeyRepository.findByUserAndEnabledTrue(user)

    @Transactional
    fun revoke(id: Long, user: User): Boolean {
        val key = apiKeyRepository.findById(id).orElse(null) ?: return false
        if (key.user?.id != user.id) return false
        key.enabled = false
        return true
    }

    @Transactional
    fun validateKey(rawKey: String): ApiKey? {
        val apiKey = apiKeyRepository.findByKeyHash(sha256(rawKey)) ?: return null
        if (!apiKey.enabled) return null
        if (apiKey.expiresAt != null && apiKey.expiresAt!!.isBefore(Instant.now())) return null
        if (apiKey.user?.enabled != true) return null

        apiKey.lastUsedAt = Instant.now()
        return apiKey
    }

    private fun generateRawKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return "sms_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.getEncoder().encodeToString(digest.digest(input.toByteArray(Charsets.UTF_8)))
    }
}
