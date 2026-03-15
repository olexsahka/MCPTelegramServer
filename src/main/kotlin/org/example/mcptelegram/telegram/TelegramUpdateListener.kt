package org.example.mcptelegram.telegram

import org.example.mcptelegram.messaging.RedisPubSubService
import org.example.mcptelegram.messaging.dto.DialogDto
import org.example.mcptelegram.messaging.dto.MessageDto
import org.example.mcptelegram.messaging.dto.TelegramUpdateEvent
import org.example.mcptelegram.telegram.model.TelegramUpdate
import org.springframework.stereotype.Component

@Component
class TelegramUpdateListener(
    private val telegramClient: TelegramClient,
    private val redisPubSubService: RedisPubSubService
) {
    init {
        telegramClient.setUpdateHandler { update ->
            when (update) {
                is TelegramUpdate.NewMessage -> {
                    val messageDto = MessageDto(
                        messageId = update.message.messageId,
                        chatId = update.message.chatId,
                        senderName = update.message.senderName,
                        text = update.message.text,
                        sentAt = update.message.date
                    )
                    redisPubSubService.publish(
                        TelegramUpdateEvent(type = TelegramUpdateEvent.EventType.NEW_MESSAGE, message = messageDto)
                    )
                }
                is TelegramUpdate.ChatUpdated -> {
                    val dialogDto = DialogDto(
                        chatId = update.dialog.chatId,
                        type = update.dialog.type.name,
                        title = update.dialog.title
                    )
                    redisPubSubService.publish(
                        TelegramUpdateEvent(type = TelegramUpdateEvent.EventType.CHAT_UPDATED, dialog = dialogDto)
                    )
                }
            }
        }
    }
}
