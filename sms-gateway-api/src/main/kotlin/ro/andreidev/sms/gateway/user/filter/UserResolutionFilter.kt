package ro.andreidev.sms.gateway.user.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import ro.andreidev.sms.gateway.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class UserResolutionFilter(
    private val userService: UserService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(UserResolutionFilter::class.java)

    companion object {
        const val USER_ATTRIBUTE = "resolvedUser"
        private val JWT_PATHS = listOf("/api/v1/api-keys", "/api/v1/admin", "/api/v1/sms", "/api/v1/portability")
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        JWT_PATHS.none { request.requestURI.startsWith(it) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication !is JwtAuthenticationToken) {
            filterChain.doFilter(request, response)
            return
        }

        val user = userService.resolveFromJwt(authentication.token)

        if (!user.enabled) {
            log.warn("Disabled user attempted access: {} ({})", user.username, user.externalProviderId)
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User account is disabled")
            return
        }

        request.setAttribute(USER_ATTRIBUTE, user)
        filterChain.doFilter(request, response)
    }
}
