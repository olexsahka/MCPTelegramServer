package org.example.mcptelegram.telegram.model

sealed class TelegramUpdate {
    data class NewMessage(val message: Message) : TelegramUpdate()
    data class ChatUpdated(val dialog: Dialog) : TelegramUpdate()
}
