package org.example.mcptelegram.mcp.tools

import kotlinx.coroutines.test.runTest
import org.example.mcptelegram.telegram.TelegramClient
import org.example.mcptelegram.telegram.model.Message
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime

class GetLastMessagesToolTest {

    private val telegramClient: TelegramClient = mock()
    private val tool = GetLastMessagesTool(telegramClient)

    @Test
    fun `should fetch messages from telegram client`() = runTest {
        val messages = listOf(
            Message(messageId = 2L, chatId = 200L, senderName = "Bob", text = "Hello", date = LocalDateTime.now())
        )
        whenever(telegramClient.getMessages(200L, 10)).thenReturn(messages)

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(mapOf("dialog_id" to 200L)) as List<Map<String, Any?>>

        assertEquals(1, result.size)
        assertEquals(2L, result[0]["message_id"])
        assertEquals("Hello", result[0]["text"])
        verify(telegramClient).getMessages(200L, 10)
    }

    @Test
    fun `should use custom count parameter`() = runTest {
        whenever(telegramClient.getMessages(100L, 5)).thenReturn(emptyList())

        tool.execute(mapOf("dialog_id" to 100L, "count" to 5))

        verify(telegramClient).getMessages(100L, 5)
    }

    @Test
    fun `should throw when dialog_id is missing`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { tool.execute(emptyMap()) }
        }
    }
}
