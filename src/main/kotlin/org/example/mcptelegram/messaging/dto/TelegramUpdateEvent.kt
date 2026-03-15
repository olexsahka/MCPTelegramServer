package org.example.mcptelegram.messaging.dto

data class TelegramUpdateEvent(
    val type: EventType,
    val message: MessageDto? = null,
    val dialog: DialogDto? = null
) {
    enum class EventType {
        NEW_MESSAGE, CHAT_UPDATED
    }
}
