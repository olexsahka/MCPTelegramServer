package org.example.mcptelegram.mcp.tools

import kotlinx.coroutines.test.runTest
import org.example.mcptelegram.telegram.TelegramClient
import org.example.mcptelegram.telegram.model.Message
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime

class GetNewMessagesToolTest {

    private val telegramClient: TelegramClient = mock()
    private val tool = GetNewMessagesTool(telegramClient)

    @Test
    fun `should return unread messages as maps`() = runTest {
        val messages = listOf(
            Message(messageId = 1L, chatId = 100L, senderId = 42L, senderName = "Alice", text = "Hey", date = LocalDateTime.now())
        )
        whenever(telegramClient.getUnreadMessages()).thenReturn(messages)

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(emptyMap()) as List<Map<String, Any?>>

        assertEquals(1, result.size)
        assertEquals(1L, result[0]["message_id"])
        assertEquals("Hey", result[0]["text"])
        assertEquals("Alice", result[0]["sender_name"])
        verify(telegramClient).getUnreadMessages()
    }

    @Test
    fun `should return empty list when no unread messages`() = runTest {
        whenever(telegramClient.getUnreadMessages()).thenReturn(emptyList())

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(emptyMap()) as List<*>

        assertTrue(result.isEmpty())
    }
}
