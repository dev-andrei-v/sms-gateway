package ro.andreidev.sms.gateway.sms.service

import ro.andreidev.sms.gateway.plan.entity.QuotaType
import ro.andreidev.sms.gateway.sms.repository.MessageRepository
import ro.andreidev.sms.gateway.user.entity.User
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@Service
class QuotaService(
    private val messageRepository: MessageRepository,
) {
    fun check(user: User): QuotaCheckResult {
        val plan = user.plan ?: return QuotaCheckResult.NoPlan
        if (!plan.isActive) return QuotaCheckResult.PlanInactive

        // Rate limit: minimum delay between messages
        if (plan.minDelaySeconds != null && plan.minDelaySeconds!! > 0) {
            val lastMessage = messageRepository.findFirstByUserOrderByCreateDateDesc(user)
            if (lastMessage != null) {
                val elapsed = Duration.between(lastMessage.createDate, Instant.now()).seconds
                if (elapsed < plan.minDelaySeconds!!) {
                    val wait = plan.minDelaySeconds!! - elapsed.toInt()
                    return QuotaCheckResult.RateLimited(waitSeconds = wait)
                }
            }
        }

        // Quota limit: message count within window
        if (plan.quotaType == QuotaType.UNLIMITED) return QuotaCheckResult.Allowed

        val limit = plan.quotaLimit ?: return QuotaCheckResult.Allowed
        val count = when (plan.quotaType) {
            QuotaType.PER_DAY -> {
                val startOfDay = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC).toInstant()
                messageRepository.countByUserAndCreateDateAfter(user, startOfDay)
            }
            QuotaType.PER_MONTH -> {
                val startOfMonth = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
                    .withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                messageRepository.countByUserAndCreateDateAfter(user, startOfMonth)
            }
            QuotaType.TOTAL -> messageRepository.countByUser(user)
            QuotaType.UNLIMITED -> return QuotaCheckResult.Allowed
        }

        return if (count >= limit) {
            QuotaCheckResult.Exceeded(used = count, limit = limit, quotaType = plan.quotaType)
        } else {
            QuotaCheckResult.Allowed
        }
    }

    sealed class QuotaCheckResult {
        data object Allowed : QuotaCheckResult()
        data object NoPlan : QuotaCheckResult()
        data object PlanInactive : QuotaCheckResult()
        data class RateLimited(val waitSeconds: Int) : QuotaCheckResult()
        data class Exceeded(val used: Long, val limit: Int, val quotaType: QuotaType) : QuotaCheckResult()
    }
}
