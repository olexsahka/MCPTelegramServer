package org.example.mcptelegram.mcp.tools

import kotlinx.coroutines.test.runTest
import org.example.mcptelegram.telegram.TelegramClient
import org.example.mcptelegram.telegram.model.ChatType
import org.example.mcptelegram.telegram.model.Dialog
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class SearchDialogToolTest {

    private val telegramClient: TelegramClient = mock()
    private val tool = SearchDialogTool(telegramClient)

    @Test
    fun `should return matching dialogs`() = runTest {
        val dialogs = listOf(
            Dialog(chatId = 99L, type = ChatType.PRIVATE, title = "John Work", unreadCount = 5)
        )
        whenever(telegramClient.searchDialog("John")).thenReturn(dialogs)

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(mapOf("query" to "John")) as List<Map<String, Any?>>

        assertEquals(1, result.size)
        assertEquals(99L, result[0]["chat_id"])
        assertEquals("John Work", result[0]["title"])
        assertEquals(5, result[0]["unread_count"])
        verify(telegramClient).searchDialog("John")
    }

    @Test
    fun `should return empty list when no match`() = runTest {
        whenever(telegramClient.searchDialog("nobody")).thenReturn(emptyList())

        @Suppress("UNCHECKED_CAST")
        val result = tool.execute(mapOf("query" to "nobody")) as List<*>

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should throw when query is missing`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { tool.execute(emptyMap()) }
        }
    }
}
