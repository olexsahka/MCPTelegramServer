package org.example.mcptelegram.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "dialogs")
class DialogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    val telegramChatId: Long,

    @Column(name = "type", nullable = false)
    val type: String,

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(name = "last_message_at")
    var lastMessageAt: LocalDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
