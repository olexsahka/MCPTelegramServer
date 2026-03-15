package org.example.mcptelegram.telegram.model

import java.time.LocalDateTime

data class Message(
    val messageId: Long,
    val chatId: Long,
    val senderId: Long? = null,
    val senderName: String? = null,
    val text: String,
    val date: LocalDateTime
)
