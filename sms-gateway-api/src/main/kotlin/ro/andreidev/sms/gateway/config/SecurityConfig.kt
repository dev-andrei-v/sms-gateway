package ro.andreidev.sms.gateway.config

import ro.andreidev.sms.gateway.apikey.filter.ApiKeyAuthFilter
import ro.andreidev.sms.gateway.internal.filter.AuthentikOtpTokenFilter
import ro.andreidev.sms.gateway.user.filter.UserResolutionFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authorization.AuthorizationManagers
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.IpAddressAuthorizationManager.hasIpAddress
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    @Qualifier("corsConfigurationSource") private val corsSource: CorsConfigurationSource,
    private val jwtConfig: JwtConfig,
    private val authentikOtpTokenFilter: AuthentikOtpTokenFilter,
    private val userResolutionFilter: UserResolutionFilter,
    private val apiKeyAuthFilter: ApiKeyAuthFilter,
) {

    /** Chain 1: Internal Authentik OTP — static Bearer token */
    @Bean
    @Order(1)
    fun internalOtpChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/internal/authentik/otp")
            .statelessDefaults()
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .addFilterBefore(authentikOtpTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    /** Device WebSocket — auth happens in DeviceWebSocketHandshakeInterceptor (Bearer token). */
    @Bean
    @Order(2)
    fun wsChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/ws/device", "/ws/device/**", "/ws/sms", "/ws/sms/**")
            .statelessDefaults()
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()

    /** Chain 3: API key management — Authentik JWT, requires sms_gateway_user or sms_gateway_admin */
    @Bean
    @Order(3)
    fun apiKeyManagementChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/api/v1/api-keys/**")
            .statelessDefaults()
            .authorizeHttpRequests { it.anyRequest().hasAnyRole("sms_gateway_user", "sms_gateway_admin") }
            .addFilterAfter(userResolutionFilter, BearerTokenAuthenticationFilter::class.java)
            .jwtResourceServer()
            .build()

    /** Chain 4: Admin — Authentik JWT, requires sms_gateway_admin */
    @Bean
    @Order(4)
    fun adminChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/api/v1/admin/**")
            .statelessDefaults()
            .authorizeHttpRequests { it.anyRequest().hasRole("sms_gateway_admin") }
            .addFilterAfter(userResolutionFilter, BearerTokenAuthenticationFilter::class.java)
            .jwtResourceServer()
            .build()

    /** Chain 5: SMS + Portability API — X-API-Key header or JWT Bearer token */
    @Bean
    @Order(5)
    fun smsApiChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/api/v1/sms/**", "/api/v1/portability/**")
            .statelessDefaults()
            .authorizeHttpRequests { it.anyRequest().hasAnyRole("sms_gateway_user", "sms_gateway_admin", "API_USER") }
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .jwtResourceServer()
            .addFilterAfter(userResolutionFilter, BearerTokenAuthenticationFilter::class.java)
            .build()

    /** Chain 6: Default — health endpoint (IP-restricted) + deny all */
    @Bean
    @Order(6)
    fun defaultChain(http: HttpSecurity): SecurityFilterChain =
        http
            .statelessDefaults()
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health").access(
                        AuthorizationManagers.anyOf(
                            hasIpAddress("192.168.14.0/24"),
                            hasIpAddress("127.0.0.1"),
                            hasIpAddress("::1"),
                            hasIpAddress("172.16.0.0/12"),
                            hasIpAddress("10.0.0.0/8")
                        )
                    )
                    .anyRequest().denyAll()
            }
            .build()

    private fun HttpSecurity.statelessDefaults(): HttpSecurity = this
        .cors { it.configurationSource(corsSource) }
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

    private fun HttpSecurity.jwtResourceServer(): HttpSecurity = this
        .oauth2ResourceServer { rs ->
            rs.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtConfig.authConverter()) }
            rs.authenticationEntryPoint(jwtConfig.authEntryPoint())
            rs.accessDeniedHandler(jwtConfig.accessDeniedHandler())
        }
}
