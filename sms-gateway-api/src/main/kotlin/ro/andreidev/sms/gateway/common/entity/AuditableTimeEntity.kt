package ro.andreidev.sms.gateway.common.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PreUpdate
import java.time.Instant

@MappedSuperclass
abstract class AuditableTimeEntity : BaseEntity() {
    @Column(nullable = false, updatable = false)
    var createDate: Instant = Instant.now()
    var updateDate: Instant? = null

    @PreUpdate
    fun onUpdate() {
        updateDate = Instant.now()
    }
}
