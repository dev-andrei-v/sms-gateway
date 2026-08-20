package ro.andreidev.sms.gateway.internal.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthentikOtpTokenFilter(
    @param:Value("\${sms-gateway.authentik.token}") private val expectedToken: String
) : OncePerRequestFilter() {
    init {
        require(expectedToken.isNotBlank()) { "Authentik OTP token must not be blank" }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI != "/internal/authentik/otp"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val auth = request.getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
        val token = auth.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")?.trim()

        if (token.isNullOrBlank() || token != expectedToken) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
            return
        }

        val authentication = UsernamePasswordAuthenticationToken(
            "authentik",
            null,
            listOf(SimpleGrantedAuthority("ROLE_INTERNAL"))
        )
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }
}
