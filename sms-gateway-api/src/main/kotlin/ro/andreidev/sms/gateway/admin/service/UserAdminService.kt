package ro.andreidev.sms.gateway.admin.service

import ro.andreidev.sms.gateway.plan.repository.PlanRepository
import ro.andreidev.sms.gateway.user.entity.User
import ro.andreidev.sms.gateway.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAdminService(
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
) {
    fun findAll(): List<User> = userRepository.findAll().toList()

    fun findById(id: Long): User? = userRepository.findById(id).orElse(null)

    @Transactional
    fun assignPlan(userId: Long, planCode: String): User? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        val plan = planRepository.findByCode(planCode)
            ?: throw IllegalArgumentException("Plan with code '$planCode' not found")
        user.plan = plan
        return user
    }

    @Transactional
    fun removePlan(userId: Long): User? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        user.plan = null
        return user
    }

    @Transactional
    fun setEnabled(userId: Long, enabled: Boolean): User? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        user.enabled = enabled
        return user
    }
}
