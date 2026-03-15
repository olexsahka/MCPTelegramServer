package org.example.mcptelegram.messaging.dto

import java.time.LocalDateTime

data class MessageDto(
    val messageId: Long,
    val chatId: Long,
    val senderName: String?,
    val text: String,
    val sentAt: LocalDateTime
)
