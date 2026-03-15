package org.example.mcptelegram.messaging

import org.example.mcptelegram.messaging.dto.TelegramUpdateEvent
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class WebSocketBroker(
    private val messagingTemplate: SimpMessagingTemplate,
    private val redisPubSubService: RedisPubSubService,
    private val telegramPersistenceService: TelegramPersistenceService
) {
    init {
        redisPubSubService.subscribe { event -> onEvent(event) }
    }

    fun onEvent(event: TelegramUpdateEvent) {
        when (event.type) {
            TelegramUpdateEvent.EventType.NEW_MESSAGE -> {
                event.message?.let { messageDto ->
                    telegramPersistenceService.saveMessage(messageDto)
                    messagingTemplate.convertAndSend("/topic/messages", messageDto)
                }
            }
            TelegramUpdateEvent.EventType.CHAT_UPDATED -> {
                event.dialog?.let { dialogDto ->
                    telegramPersistenceService.updateDialog(dialogDto)
                    messagingTemplate.convertAndSend("/topic/dialogs", dialogDto)
                }
            }
        }
    }
}
