package org.example.mcptelegram.mcp.tools

import kotlinx.coroutines.test.runTest
import org.example.mcptelegram.telegram.TelegramClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class MarkAsReadToolTest {

    private val telegramClient: TelegramClient = mock()
    private val tool = MarkAsReadTool(telegramClient)

    @Test
    fun `should mark dialog as read and return ok`() = runTest {
        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(mapOf("dialog_id" to 100L)) as Map<String, Any?>

        assertEquals(true, result["ok"])
        verify(telegramClient).markAsRead(100L)
    }

    @Test
    fun `should throw when dialog_id is missing`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { tool.execute(emptyMap()) }
        }
    }
}
