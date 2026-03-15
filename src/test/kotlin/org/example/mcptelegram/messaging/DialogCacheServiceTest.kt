package org.example.mcptelegram.messaging

import org.example.mcptelegram.AbstractIntegrationTest
import org.example.mcptelegram.messaging.dto.DialogDto
import org.example.mcptelegram.messaging.dto.MessageDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class DialogCacheServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var dialogCacheService: DialogCacheService

    @Test
    fun `should cache and retrieve dialog`() {
        val dialog = DialogDto(chatId = 1001L, type = "PRIVATE", title = "Test Dialog")
        dialogCacheService.cacheDialog(1001L, dialog)

        val cached = dialogCacheService.getCachedDialog(1001L)
        assertNotNull(cached)
        assertEquals("Test Dialog", cached!!.title)
    }

    @Test
    fun `should return null for non-cached dialog`() {
        val cached = dialogCacheService.getCachedDialog(9999L)
        assertNull(cached)
    }

    @Test
    fun `should cache and retrieve last messages`() {
        val messages = listOf(
            MessageDto(messageId = 1L, chatId = 1002L, senderName = "Alice", text = "Hi", sentAt = LocalDateTime.now())
        )
        dialogCacheService.cacheLastMessages(1002L, messages)

        val cached = dialogCacheService.getCachedLastMessages(1002L)
        assertNotNull(cached)
        assertEquals(1, cached!!.size)
        assertEquals("Hi", cached[0].text)
    }

    @Test
    fun `should invalidate dialog cache`() {
        val dialog = DialogDto(chatId = 1003L, type = "GROUP", title = "Group")
        dialogCacheService.cacheDialog(1003L, dialog)
        dialogCacheService.invalidateDialog(1003L)

        val cached = dialogCacheService.getCachedDialog(1003L)
        assertNull(cached)
    }
}
