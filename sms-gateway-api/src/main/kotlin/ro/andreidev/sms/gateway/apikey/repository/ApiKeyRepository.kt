package ro.andreidev.sms.gateway.apikey.repository

import ro.andreidev.sms.gateway.apikey.entity.ApiKey
import ro.andreidev.sms.gateway.user.entity.User
import org.springframework.data.repository.CrudRepository

interface ApiKeyRepository : CrudRepository<ApiKey, Long> {
    fun findByKeyHash(keyHash: String): ApiKey?
    fun findByUserAndEnabledTrue(user: User): List<ApiKey>
}
