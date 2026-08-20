package ro.andreidev.sms.gateway.config

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class JwtConfig {

    private val log = LoggerFactory.getLogger(JwtConfig::class.java)

    fun authConverter(): JwtAuthenticationConverter {
        val authoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("groups")
            setAuthorityPrefix("ROLE_")
        }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        }
    }

    fun authEntryPoint() = AuthenticationEntryPoint { request, response, ex ->
        log.warn("401 Unauthorized: method={} uri={} msg={}", request.method, request.requestURI, ex.message)
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
    }

    fun accessDeniedHandler() = AccessDeniedHandler { request, response, ex ->
        val auth = SecurityContextHolder.getContext().authentication
        log.warn(
            "403 Forbidden: method={} uri={} principal={} authorities={} msg={}",
            request.method, request.requestURI, auth?.name, auth?.authorities?.map { it.authority }, ex.message
        )
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
    }
}
