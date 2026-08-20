package ro.andreidev.sms.gateway.admin.service

import ro.andreidev.sms.gateway.admin.dto.PlanRequest
import ro.andreidev.sms.gateway.plan.entity.Plan
import ro.andreidev.sms.gateway.plan.repository.PlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlanAdminService(
    private val planRepository: PlanRepository,
) {
    fun findAll(): List<Plan> = planRepository.findAll().toList()

    fun findById(id: Long): Plan? = planRepository.findById(id).orElse(null)

    @Transactional
    fun create(request: PlanRequest): Plan {
        return planRepository.save(
            Plan(
                code = request.code,
                name = request.name,
                quotaType = request.quotaType,
                quotaLimit = request.quotaLimit,
                minDelaySeconds = request.minDelaySeconds,
                maxRecipientsPerRequest = request.maxRecipientsPerRequest,
                isActive = request.isActive,
            )
        )
    }

    @Transactional
    fun update(id: Long, request: PlanRequest): Plan? {
        val plan = planRepository.findById(id).orElse(null) ?: return null
        plan.code = request.code
        plan.name = request.name
        plan.quotaType = request.quotaType
        plan.quotaLimit = request.quotaLimit
        plan.minDelaySeconds = request.minDelaySeconds
        plan.maxRecipientsPerRequest = request.maxRecipientsPerRequest
        plan.isActive = request.isActive
        return plan
    }

    @Transactional
    fun deactivate(id: Long): Boolean {
        val plan = planRepository.findById(id).orElse(null) ?: return false
        plan.isActive = false
        return true
    }
}
