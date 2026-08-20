package ro.andreidev.sms.gateway.apikey.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ro.andreidev.sms.gateway.common.entity.AuditableTimeEntity
import ro.andreidev.sms.gateway.user.entity.User
import java.time.Instant

@Entity
@Table(name = "api_keys")
class ApiKey(
    @Column(nullable = false, unique = true)
    var keyHash: String = "",

    @Column(nullable = false, length = 12)
    var keyPrefix: String = "",

    @Column(nullable = false)
    var name: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var user: User? = null,

    @Column(nullable = false)
    var enabled: Boolean = true,

    var expiresAt: Instant? = null,

    var lastUsedAt: Instant? = null,
) : AuditableTimeEntity()
