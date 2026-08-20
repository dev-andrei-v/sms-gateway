package ro.andreidev.sms.gateway.sms.ws

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Validates the API key the Android middleware presents at WebSocket handshake.
 *
 * Initial implementation is a single shared secret from config. For a real
 * multi-device deployment, swap this for a lookup against the existing api-keys
 * module — each device gets its own key, which lets the backend revoke a single
 * compromised device without rotating the others.
 */
@Component
class DeviceApiKeyVerifier(
    @Value("\${sms-gateway.device-ws.shared-key:}") private val sharedKey: String
) {
    fun verify(apiKey: String, deviceId: String?): Boolean {
        if (sharedKey.isBlank()) return false
        if (apiKey.isBlank()) return false
        return apiKey == sharedKey
    }
}
