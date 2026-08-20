package ro.andreidev.sms.gateway.admin.controller

import jakarta.validation.Valid
import ro.andreidev.sms.gateway.admin.dto.AssignPlanRequest
import ro.andreidev.sms.gateway.admin.dto.UpdateUserEnabledRequest
import ro.andreidev.sms.gateway.admin.dto.UserResponse
import ro.andreidev.sms.gateway.admin.service.UserAdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/users")
class UserAdminController(
    private val userAdminService: UserAdminService,
) {
    @GetMapping
    fun listUsers(): ResponseEntity<List<UserResponse>> {
        val users = userAdminService.findAll().map { UserResponse.from(it) }
        return ResponseEntity.ok(users)
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userAdminService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(UserResponse.from(user))
    }

    @PutMapping("/{id}/plan")
    fun assignPlan(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignPlanRequest
    ): ResponseEntity<Any> {
        return try {
            val user = userAdminService.assignPlan(id, request.planCode)
                ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(UserResponse.from(user))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}/plan")
    fun removePlan(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userAdminService.removePlan(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(UserResponse.from(user))
    }

    @PutMapping("/{id}/enabled")
    fun setEnabled(
        @PathVariable id: Long,
        @RequestBody request: UpdateUserEnabledRequest
    ): ResponseEntity<UserResponse> {
        val user = userAdminService.setEnabled(id, request.enabled) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(UserResponse.from(user))
    }
}
