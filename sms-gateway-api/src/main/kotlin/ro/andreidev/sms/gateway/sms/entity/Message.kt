package ro.andreidev.sms.gateway.sms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ro.andreidev.sms.gateway.common.entity.AuditableTimeEntity
import ro.andreidev.sms.gateway.user.entity.User

@Entity
@Table(name = "messages")
class Message(
    @Column(nullable = false)
    var phoneNumber: String = "",
    @Column(nullable = false)
    var content: String = "",
    @Enumerated(EnumType.STRING)
    var status: MessageStatus = MessageStatus.UNKNOWN,

    var providerId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    var user: User? = null
) : AuditableTimeEntity()
