package ro.andreidev.sms.gateway.sms.repository

import ro.andreidev.sms.gateway.sms.entity.Message
import ro.andreidev.sms.gateway.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface MessageRepository : JpaRepository<Message, Long> {
    fun countByUserAndCreateDateAfter(user: User, after: Instant): Long
    fun countByUser(user: User): Long
    fun findFirstByUserOrderByCreateDateDesc(user: User): Message?
    fun findByUser(user: User, pageable: Pageable): Page<Message>
    fun findByProviderId(providerId: String): Message?
    fun findByIdAndUser_Id(id: Long, userId: Long): Message?
}
