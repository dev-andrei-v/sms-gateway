package ro.andreidev.sms.gateway.apikey.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import ro.andreidev.sms.gateway.apikey.service.ApiKeyService
import ro.andreidev.sms.gateway.user.filter.UserResolutionFilter.Companion.USER_ATTRIBUTE
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiKeyAuthFilter(
    private val apiKeyService: ApiKeyService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(ApiKeyAuthFilter::class.java)

    companion object {
        const val API_KEY_HEADER = "X-API-Key"
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.getHeader(API_KEY_HEADER).isNullOrBlank()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val rawKey = request.getHeader(API_KEY_HEADER)!!.trim()

        val apiKey = apiKeyService.validateKey(rawKey)
        if (apiKey == null) {
            log.warn("Invalid or expired API key from {}", request.remoteAddr)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired API key")
            return
        }

        val user = apiKey.user!!
        val authentication = UsernamePasswordAuthenticationToken(
            user.username,
            null,
            listOf(SimpleGrantedAuthority("ROLE_API_USER"))
        )
        SecurityContextHolder.getContext().authentication = authentication
        request.setAttribute(USER_ATTRIBUTE, user)

        filterChain.doFilter(request, response)
    }
}
