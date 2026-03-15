package org.example.mcptelegram.mcp.tools

import kotlinx.coroutines.test.runTest
import org.example.mcptelegram.telegram.TelegramClient
import org.example.mcptelegram.telegram.model.ChatType
import org.example.mcptelegram.telegram.model.Dialog
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class GetDialogsToolTest {

    private val telegramClient: TelegramClient = mock()
    private val tool = GetDialogsTool(telegramClient)

    @Test
    fun `should return dialogs from telegram client`() = runTest {
        val dialogs = listOf(
            Dialog(chatId = 1L, type = ChatType.PRIVATE, title = "Alice", unreadCount = 3),
            Dialog(chatId = 2L, type = ChatType.GROUP, title = "Team", unreadCount = 0)
        )
        whenever(telegramClient.getDialogs(100)).thenReturn(dialogs)

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(emptyMap()) as List<Map<String, Any?>>

        assertEquals(2, result.size)
        assertEquals(1L, result[0]["chat_id"])
        assertEquals("Alice", result[0]["title"])
        assertEquals(3, result[0]["unread_count"])
        verify(telegramClient).getDialogs(100)
    }

    @Test
    fun `should pass custom limit to telegram client`() = runTest {
        whenever(telegramClient.getDialogs(10)).thenReturn(emptyList())

        tool.execute(mapOf("limit" to 10))

        verify(telegramClient).getDialogs(10)
    }
}
