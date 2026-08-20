package ro.andreidev.sms.gateway.user.repository

import ro.andreidev.sms.gateway.user.entity.User
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Long> {
    fun findByExternalProviderId(externalProviderId: String): User?
}
