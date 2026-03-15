package org.example.mcptelegram.telegram.model

data class Dialog(
    val chatId: Long,
    val type: ChatType,
    val title: String,
    val unreadCount: Int = 0,
    val lastMessage: Message? = null
)
