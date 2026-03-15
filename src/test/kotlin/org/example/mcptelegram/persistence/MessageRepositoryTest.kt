package org.example.mcptelegram.persistence

import org.example.mcptelegram.AbstractIntegrationTest
import org.example.mcptelegram.persistence.entity.DialogEntity
import org.example.mcptelegram.persistence.entity.MessageEntity
import org.example.mcptelegram.persistence.repository.DialogRepository
import org.example.mcptelegram.persistence.repository.MessageRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class MessageRepositoryTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var messageRepository: MessageRepository

    @Autowired
    lateinit var dialogRepository: DialogRepository

    @Test
    fun `should save message and find by telegramMessageId`() {
        val dialog = dialogRepository.save(DialogEntity(telegramChatId = 301L, type = "PRIVATE", title = "Test"))
        val message = MessageEntity(
            dialog = dialog,
            text = "Hello",
            telegramMessageId = 555L,
            sentAt = LocalDateTime.now()
        )
        messageRepository.save(message)

        val found = messageRepository.findByTelegramMessageId(555L)
        assertTrue(found.isPresent)
        assertEquals("Hello", found.get().text)
    }

    @Test
    fun `should find messages by dialog ordered by sentAt desc with pagination`() {
        val dialog = dialogRepository.save(DialogEntity(telegramChatId = 302L, type = "GROUP", title = "Group"))
        val now = LocalDateTime.now()
        messageRepository.saveAll(listOf(
            MessageEntity(dialog = dialog, text = "First", telegramMessageId = 1L, sentAt = now.minusMinutes(10)),
            MessageEntity(dialog = dialog, text = "Second", telegramMessageId = 2L, sentAt = now.minusMinutes(5)),
            MessageEntity(dialog = dialog, text = "Third", telegramMessageId = 3L, sentAt = now)
        ))

        val result = messageRepository.findByDialogOrderBySentAtDesc(dialog, PageRequest.of(0, 2))
        assertEquals(2, result.size)
        assertEquals("Third", result[0].text)
        assertEquals("Second", result[1].text)
    }
}
