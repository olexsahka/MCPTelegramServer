package org.example.mcptelegram.telegram

import org.example.mcptelegram.messaging.RedisPubSubService
import org.example.mcptelegram.messaging.dto.TelegramUpdateEvent
import org.example.mcptelegram.telegram.model.ChatType
import org.example.mcptelegram.telegram.model.Dialog
import org.example.mcptelegram.telegram.model.Message
import org.example.mcptelegram.telegram.model.TelegramUpdate
import org.example.mcptelegram.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDateTime

class TelegramUpdateListenerTest : AbstractIntegrationTest() {

    @MockBean
    lateinit var telegramClient: TelegramClient

    @MockBean
    lateinit var redisPubSubService: RedisPubSubService

    @Autowired
    lateinit var telegramUpdateListener: TelegramUpdateListener

    @Test
    fun `should publish NEW_MESSAGE event when new message received`() {
        val handlerCaptor = argumentCaptor<(TelegramUpdate) -> Unit>()
        verify(telegramClient).setUpdateHandler(handlerCaptor.capture())

        val message = Message(messageId = 1L, chatId = 100L, senderName = "Alice", text = "Hi", date = LocalDateTime.now())
        handlerCaptor.firstValue.invoke(TelegramUpdate.NewMessage(message))

        verify(redisPubSubService).publish(argThat { type == TelegramUpdateEvent.EventType.NEW_MESSAGE })
    }

    @Test
    fun `should publish CHAT_UPDATED event when chat updated`() {
        val handlerCaptor = argumentCaptor<(TelegramUpdate) -> Unit>()
        verify(telegramClient).setUpdateHandler(handlerCaptor.capture())

        val dialog = Dialog(chatId = 200L, type = ChatType.GROUP, title = "My Group")
        handlerCaptor.firstValue.invoke(TelegramUpdate.ChatUpdated(dialog))

        verify(redisPubSubService).publish(argThat { type == TelegramUpdateEvent.EventType.CHAT_UPDATED })
    }
}
