package ro.andreidev.sms.gateway.portability.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class PortabilityCacheConfig {
    @Bean
    fun portabilityCacheManager(): CacheManager =
        CaffeineCacheManager(PORTABILITY_LOOKUP_CACHE).apply {
            setCaffeine(
                Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofDays(1))
                    .maximumSize(10_000)
            )
        }

    companion object {
        const val PORTABILITY_LOOKUP_CACHE = "portability-lookup"
    }
}
