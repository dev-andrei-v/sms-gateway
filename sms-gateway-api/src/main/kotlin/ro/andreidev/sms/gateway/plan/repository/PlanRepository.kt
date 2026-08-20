package ro.andreidev.sms.gateway.plan.repository

import ro.andreidev.sms.gateway.plan.entity.Plan
import org.springframework.data.repository.CrudRepository

interface PlanRepository : CrudRepository<Plan, Long> {
    fun findByCode(code: String): Plan?
    fun findByIsActiveTrue(): List<Plan>
}
