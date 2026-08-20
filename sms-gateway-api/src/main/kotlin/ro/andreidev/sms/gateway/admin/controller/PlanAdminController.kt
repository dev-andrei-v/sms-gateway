package ro.andreidev.sms.gateway.admin.controller

import jakarta.validation.Valid
import ro.andreidev.sms.gateway.admin.dto.PlanRequest
import ro.andreidev.sms.gateway.admin.dto.PlanResponse
import ro.andreidev.sms.gateway.admin.service.PlanAdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/plans")
class PlanAdminController(
    private val planAdminService: PlanAdminService,
) {
    @GetMapping
    fun listPlans(): ResponseEntity<List<PlanResponse>> {
        val plans = planAdminService.findAll().map { PlanResponse.from(it) }
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/{id}")
    fun getPlan(@PathVariable id: Long): ResponseEntity<PlanResponse> {
        val plan = planAdminService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(PlanResponse.from(plan))
    }

    @PostMapping
    fun createPlan(@Valid @RequestBody request: PlanRequest): ResponseEntity<PlanResponse> {
        val plan = planAdminService.create(request)
        return ResponseEntity.status(201).body(PlanResponse.from(plan))
    }

    @PutMapping("/{id}")
    fun updatePlan(
        @PathVariable id: Long,
        @Valid @RequestBody request: PlanRequest
    ): ResponseEntity<PlanResponse> {
        val plan = planAdminService.update(id, request) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(PlanResponse.from(plan))
    }

    @DeleteMapping("/{id}")
    fun deactivatePlan(@PathVariable id: Long): ResponseEntity<Void> {
        return if (planAdminService.deactivate(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
