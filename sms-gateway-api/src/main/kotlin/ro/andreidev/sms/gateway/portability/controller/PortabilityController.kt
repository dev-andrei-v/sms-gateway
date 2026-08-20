package ro.andreidev.sms.gateway.portability.controller

import ro.andreidev.sms.gateway.portability.service.InvalidPhoneNumberException
import ro.andreidev.sms.gateway.portability.service.PortabilityLookupException
import ro.andreidev.sms.gateway.portability.service.PortabilityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/portability")
class PortabilityController(
    private val portabilityService: PortabilityService,
) {
    @GetMapping("/{phoneNumber}")
    fun lookup(
        @PathVariable phoneNumber: String,
    ): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(portabilityService.lookup(phoneNumber))
        } catch (ex: InvalidPhoneNumberException) {
            ResponseEntity.badRequest().body(mapOf("message" to (ex.message ?: "Invalid request.")))
        } catch (ex: PortabilityLookupException) {
            ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(mapOf("message" to (ex.message ?: "Failed to fetch upstream page.")))
        }
}
