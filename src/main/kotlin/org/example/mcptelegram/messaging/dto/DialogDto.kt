package org.example.mcptelegram.messaging.dto

import java.time.LocalDateTime

data class DialogDto(
    val chatId: Long,
    val type: String,
    val title: String,
    val lastMessageAt: LocalDateTime? = null
)
