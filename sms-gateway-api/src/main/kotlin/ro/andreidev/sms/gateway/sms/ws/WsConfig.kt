package ro.andreidev.sms.gateway.sms.ws

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WsConfig(
    private val deviceHandler: DeviceWebSocketHandler,
    private val deviceInterceptor: DeviceWebSocketHandshakeInterceptor,
    private val browserHandler: BrowserSmsStatusWebSocketHandler,
    private val browserInterceptor: BrowserSmsStatusHandshakeInterceptor
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(deviceHandler, "/ws/device")
            .addInterceptors(deviceInterceptor)
            .setAllowedOriginPatterns("*")

        registry.addHandler(browserHandler, "/ws/sms")
            .addInterceptors(browserInterceptor)
            .setAllowedOriginPatterns("*")
    }
}
