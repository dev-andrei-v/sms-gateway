package ro.andreidev.sms.gateway.plan.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import ro.andreidev.sms.gateway.common.entity.AuditableTimeEntity

@Entity
@Table(name = "plans")
class Plan(
    @Column(unique = true, nullable = false)
    var code: String = "",
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var quotaType: QuotaType = QuotaType.UNLIMITED,
    var quotaLimit: Int? = null,
    var minDelaySeconds: Int? = null,
    var maxRecipientsPerRequest: Int? = null,
    @Column(nullable = false)
    var isActive: Boolean = true,
) : AuditableTimeEntity()
