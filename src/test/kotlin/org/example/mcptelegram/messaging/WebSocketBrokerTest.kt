package org.example.mcptelegram.messaging

import org.example.mcptelegram.messaging.dto.DialogDto
import org.example.mcptelegram.messaging.dto.MessageDto
import org.example.mcptelegram.messaging.dto.TelegramUpdateEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.LocalDateTime

class WebSocketBrokerTest {

    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var redisPubSubService: RedisPubSubService
    private lateinit var telegramPersistenceService: TelegramPersistenceService
    private lateinit var webSocketBroker: WebSocketBroker

    @BeforeEach
    fun setUp() {
        messagingTemplate = mock()
        redisPubSubService = mock()
        telegramPersistenceService = mock()
        webSocketBroker = WebSocketBroker(messagingTemplate, redisPubSubService, telegramPersistenceService)
    }

    @Test
    fun `should send message to topic on NEW_MESSAGE event`() {
        val messageDto = MessageDto(messageId = 1L, chatId = 100L, senderName = "Alice", text = "Hi", sentAt = LocalDateTime.now())
        val event = TelegramUpdateEvent(type = TelegramUpdateEvent.EventType.NEW_MESSAGE, message = messageDto)

        webSocketBroker.onEvent(event)

        verify(messagingTemplate).convertAndSend("/topic/messages", messageDto)
        verify(telegramPersistenceService).saveMessage(messageDto)
    }

    @Test
    fun `should send dialog to topic on CHAT_UPDATED event`() {
        val dialogDto = DialogDto(chatId = 200L, type = "GROUP", title = "My Group")
        val event = TelegramUpdateEvent(type = TelegramUpdateEvent.EventType.CHAT_UPDATED, dialog = dialogDto)

        webSocketBroker.onEvent(event)

        verify(messagingTemplate).convertAndSend("/topic/dialogs", dialogDto)
        verify(telegramPersistenceService).updateDialog(dialogDto)
    }
}
