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
    fun `should return unread messages as maps with default limit`() = runTest {
        val messages = listOf(
            Message(messageId = 1L, chatId = 100L, senderId = 42L, senderName = "Alice", text = "Hey", date = LocalDateTime.now())
        )
        whenever(telegramClient.getUnreadMessages(50)).thenReturn(messages)

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(emptyMap()) as List<Map<String, Any?>>

        assertEquals(1, result.size)
        assertEquals(1L, result[0]["message_id"])
        assertEquals("Hey", result[0]["text"])
        assertEquals("Alice", result[0]["sender_name"])
        verify(telegramClient).getUnreadMessages(50)
    }

    @Test
    fun `should pass custom limit to telegram client`() = runTest {
        whenever(telegramClient.getUnreadMessages(10)).thenReturn(emptyList())

        tool.execute(mapOf("limit" to 10))

        verify(telegramClient).getUnreadMessages(10)
    }

    @Test
    fun `should return empty list when no unread messages`() = runTest {
        whenever(telegramClient.getUnreadMessages(50)).thenReturn(emptyList())

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(emptyMap()) as List<*>

        assertTrue(result.isEmpty())
    }
}
