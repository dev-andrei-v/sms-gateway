package ro.andreidev.sms.gateway.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ro.andreidev.sms.gateway.common.entity.AuditableTimeEntity
import ro.andreidev.sms.gateway.plan.entity.Plan

@Entity
@Table(name = "users")
class User(
    @Column(unique = true, nullable = false)
    var externalProviderId: String = "",

    @Column(nullable = false)
    var username: String = "",

    @Column(nullable = false)
    var enabled: Boolean = true,

    @ManyToOne(fetch = FetchType.LAZY)
    var plan: Plan? = null,
) : AuditableTimeEntity()
