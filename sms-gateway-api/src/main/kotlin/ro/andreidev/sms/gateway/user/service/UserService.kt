package ro.andreidev.sms.gateway.user.service

import ro.andreidev.sms.gateway.user.entity.User
import ro.andreidev.sms.gateway.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    @Transactional
    fun resolveFromJwt(jwt: Jwt): User {
        val externalId = jwt.subject
        val username = jwt.getClaimAsString("preferred_username") ?: externalId

        val existing = userRepository.findByExternalProviderId(externalId)
        if (existing != null) {
            if (existing.username != username) {
                log.info("Syncing username for user {} from '{}' to '{}'", externalId, existing.username, username)
                existing.username = username
            }
            return existing
        }

        return try {
            val newUser = userRepository.save(User(externalProviderId = externalId, username = username))
            log.info("Auto-provisioned new user: {} ({})", username, externalId)
            newUser
        } catch (e: DataIntegrityViolationException) {
            log.debug("User {} already exists (concurrent creation), fetching", externalId)
            userRepository.findByExternalProviderId(externalId)!!
        }
    }
}
