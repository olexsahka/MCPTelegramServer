package org.example.mcptelegram.messaging

import org.example.mcptelegram.AbstractIntegrationTest
import org.example.mcptelegram.messaging.dto.DialogDto
import org.example.mcptelegram.messaging.dto.MessageDto
import org.example.mcptelegram.persistence.repository.DialogRepository
import org.example.mcptelegram.persistence.repository.MessageRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class TelegramPersistenceServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var telegramPersistenceService: TelegramPersistenceService

    @Autowired
    lateinit var dialogRepository: DialogRepository

    @Autowired
    lateinit var messageRepository: MessageRepository

    @Test
    fun `should save message and create dialog if not exists`() {
        val messageDto = MessageDto(
            messageId = 100L, chatId = 500L, senderName = "Bob",
            text = "Test message", sentAt = LocalDateTime.now()
        )
        telegramPersistenceService.saveMessage(messageDto)

        val dialog = dialogRepository.findByTelegramChatId(500L)
        assertTrue(dialog.isPresent)

        val messages = messageRepository.findByTelegramMessageId(100L)
        assertTrue(messages.isPresent)
        assertEquals("Test message", messages.get().text)
    }

    @Test
    fun `should not duplicate message on second save`() {
        val messageDto = MessageDto(
            messageId = 101L, chatId = 501L, senderName = "Alice",
            text = "Unique message", sentAt = LocalDateTime.now()
        )
        telegramPersistenceService.saveMessage(messageDto)
        telegramPersistenceService.saveMessage(messageDto)

        val found = messageRepository.findByTelegramMessageId(101L)
        assertTrue(found.isPresent)
    }

    @Test
    fun `should create or update dialog`() {
        val dialogDto = DialogDto(chatId = 600L, type = "GROUP", title = "My Group")
        telegramPersistenceService.updateDialog(dialogDto)

        val found = dialogRepository.findByTelegramChatId(600L)
        assertTrue(found.isPresent)
        assertEquals("My Group", found.get().title)
    }
}
