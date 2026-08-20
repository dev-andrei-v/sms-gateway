package ro.andreidev.sms.gateway.admin.dto

import ro.andreidev.sms.gateway.plan.entity.Plan
import ro.andreidev.sms.gateway.plan.entity.QuotaType
import java.time.Instant

data class PlanResponse(
    val id: Long,
    val code: String,
    val name: String,
    val quotaType: QuotaType,
    val quotaLimit: Int?,
    val minDelaySeconds: Int?,
    val maxRecipientsPerRequest: Int?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(plan: Plan) = PlanResponse(
            id = plan.id!!,
            code = plan.code,
            name = plan.name,
            quotaType = plan.quotaType,
            quotaLimit = plan.quotaLimit,
            minDelaySeconds = plan.minDelaySeconds,
            maxRecipientsPerRequest = plan.maxRecipientsPerRequest,
            isActive = plan.isActive,
            createdAt = plan.createDate,
            updatedAt = plan.updateDate,
        )
    }
}
