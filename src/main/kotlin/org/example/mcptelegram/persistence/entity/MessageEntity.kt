package org.example.mcptelegram.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "messages")
class MessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialog_id", nullable = false)
    val dialog: DialogEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    val sender: UserEntity? = null,

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    val text: String,

    @Column(name = "telegram_message_id", nullable = false)
    val telegramMessageId: Long,

    @Column(name = "sent_at", nullable = false)
    val sentAt: LocalDateTime,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
