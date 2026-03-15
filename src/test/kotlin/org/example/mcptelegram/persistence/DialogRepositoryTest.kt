package org.example.mcptelegram.persistence

import org.example.mcptelegram.AbstractIntegrationTest
import org.example.mcptelegram.persistence.entity.DialogEntity
import org.example.mcptelegram.persistence.repository.DialogRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class DialogRepositoryTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var dialogRepository: DialogRepository

    @Test
    fun `should save and find dialog by telegramChatId`() {
        val dialog = DialogEntity(telegramChatId = 100L, type = "PRIVATE", title = "Test Chat")
        dialogRepository.save(dialog)

        val found = dialogRepository.findByTelegramChatId(100L)
        assertTrue(found.isPresent)
        assertEquals("Test Chat", found.get().title)
    }

    @Test
    fun `should return dialogs ordered by lastMessageAt desc`() {
        val now = LocalDateTime.now()
        val dialog1 = DialogEntity(telegramChatId = 201L, type = "PRIVATE", title = "Older", lastMessageAt = now.minusHours(2))
        val dialog2 = DialogEntity(telegramChatId = 202L, type = "GROUP", title = "Newer", lastMessageAt = now.minusHours(1))
        dialogRepository.saveAll(listOf(dialog1, dialog2))

        val result = dialogRepository.findAllByOrderByLastMessageAtDesc()
        val titles = result.map { it.title }
        assertTrue(titles.indexOf("Newer") < titles.indexOf("Older"))
    }
}
