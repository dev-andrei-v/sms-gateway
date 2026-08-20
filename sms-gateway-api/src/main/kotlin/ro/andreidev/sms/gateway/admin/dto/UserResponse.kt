package ro.andreidev.sms.gateway.admin.dto

import ro.andreidev.sms.gateway.user.entity.User
import java.time.Instant

data class UserResponse(
    val id: Long,
    val externalProviderId: String,
    val username: String,
    val enabled: Boolean,
    val planCode: String?,
    val createdAt: Instant,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id!!,
            externalProviderId = user.externalProviderId,
            username = user.username,
            enabled = user.enabled,
            planCode = user.plan?.code,
            createdAt = user.createDate,
            updatedAt = user.updateDate,
        )
    }
}
