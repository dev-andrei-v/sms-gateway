package ro.andreidev.sms.gateway.apikey.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import ro.andreidev.sms.gateway.apikey.dto.ApiKeyResponse
import ro.andreidev.sms.gateway.apikey.dto.CreateApiKeyRequest
import ro.andreidev.sms.gateway.apikey.dto.CreateApiKeyResponse
import ro.andreidev.sms.gateway.apikey.service.ApiKeyService
import ro.andreidev.sms.gateway.user.entity.User
import ro.andreidev.sms.gateway.user.filter.UserResolutionFilter.Companion.USER_ATTRIBUTE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/api-keys")
class ApiKeyController(
    private val apiKeyService: ApiKeyService
) {
    @PostMapping
    fun createApiKey(
        @Valid @RequestBody body: CreateApiKeyRequest,
        request: HttpServletRequest
    ): ResponseEntity<CreateApiKeyResponse> {
        val user = request.getAttribute(USER_ATTRIBUTE) as User
        val (apiKey, rawKey) = apiKeyService.create(user, body.name, body.expiresAt)

        return ResponseEntity.ok(
            CreateApiKeyResponse(
                id = apiKey.id!!,
                name = apiKey.name,
                key = rawKey,
                prefix = apiKey.keyPrefix,
                expiresAt = apiKey.expiresAt,
                createdAt = apiKey.createDate,
            )
        )
    }

    @GetMapping
    fun listApiKeys(request: HttpServletRequest): ResponseEntity<List<ApiKeyResponse>> {
        val user = request.getAttribute(USER_ATTRIBUTE) as User
        val keys = apiKeyService.listByUser(user).map { key ->
            ApiKeyResponse(
                id = key.id!!,
                name = key.name,
                prefix = key.keyPrefix,
                enabled = key.enabled,
                expiresAt = key.expiresAt,
                lastUsedAt = key.lastUsedAt,
                createdAt = key.createDate,
            )
        }
        return ResponseEntity.ok(keys)
    }

    @DeleteMapping("/{id}")
    fun revokeApiKey(
        @PathVariable id: Long,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        val user = request.getAttribute(USER_ATTRIBUTE) as User
        return if (apiKeyService.revoke(id, user)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
