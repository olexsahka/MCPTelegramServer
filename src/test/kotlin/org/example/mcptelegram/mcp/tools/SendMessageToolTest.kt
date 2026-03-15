package org.example.mcptelegram.mcp.tools

import kotlinx.coroutines.test.runTest
import org.example.mcptelegram.telegram.TelegramClient
import org.example.mcptelegram.telegram.model.Message
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime

class SendMessageToolTest {

    private val telegramClient: TelegramClient = mock()
    private val tool = SendMessageTool(telegramClient)

    @Test
    fun `should send message and return result`() = runTest {
        val sentMessage = Message(messageId = 10L, chatId = 100L, text = "Hello", date = LocalDateTime.now())
        whenever(telegramClient.sendMessage(100L, "Hello")).thenReturn(sentMessage)

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(mapOf("dialog_id" to 100L, "text" to "Hello")) as Map<String, Any?>

        assertEquals(10L, result["message_id"])
        assertEquals(100L, result["chat_id"])
        assertEquals("Hello", result["text"])
        verify(telegramClient).sendMessage(100L, "Hello")
    }

    @Test
    fun `should throw when dialog_id is missing`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { tool.execute(mapOf("text" to "Hi")) }
        }
    }

    @Test
    fun `should throw when text is missing`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { tool.execute(mapOf("dialog_id" to 100L)) }
        }
    }
}
