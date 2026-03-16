package org.example.mcptelegram.mcp.tools

import kotlinx.coroutines.test.runTest
import org.example.mcptelegram.telegram.TelegramClient
import org.example.mcptelegram.telegram.model.Message
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime

class GetMessageToolTest {

    private val telegramClient: TelegramClient = mock()
    private val tool = GetMessageTool(telegramClient)

    @Test
    fun `should return message by id`() = runTest {
        val message = Message(messageId = 42L, chatId = 100L, senderName = "Alice", text = "Hi", date = LocalDateTime.now())
        whenever(telegramClient.getMessage(100L, 42L)).thenReturn(message)

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(mapOf("dialog_id" to 100L, "message_id" to 42L)) as Map<String, Any?>

        assertEquals(42L, result["message_id"])
        assertEquals("Hi", result["text"])
        assertEquals("Alice", result["sender_name"])
    }

    @Test
    fun `should return error when message not found`() = runTest {
        whenever(telegramClient.getMessage(100L, 999L)).thenReturn(null)

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(mapOf("dialog_id" to 100L, "message_id" to 999L)) as Map<String, Any?>

        assertEquals("Message not found", result["error"])
    }

    @Test
    fun `should throw when dialog_id is missing`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { tool.execute(mapOf("message_id" to 1L)) }
        }
    }

    @Test
    fun `should throw when message_id is missing`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { tool.execute(mapOf("dialog_id" to 100L)) }
        }
    }
}
