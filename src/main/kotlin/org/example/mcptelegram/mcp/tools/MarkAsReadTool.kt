package org.example.mcptelegram.mcp.tools

import org.example.mcptelegram.mcp.McpToolHandler
import org.example.mcptelegram.telegram.TelegramClient
import org.springframework.stereotype.Component

@Component
class MarkAsReadTool(
    private val telegramClient: TelegramClient
) : McpToolHandler {

    override val name = "mark_as_read"
    override val description = "Mark all messages in a dialog as read"
    override val inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "dialog_id" to mapOf("type" to "number", "description" to "Telegram chat ID")
        ),
        "required" to listOf("dialog_id")
    )

    override suspend fun execute(params: Map<String, Any?>): Any {
        val chatId = (params["dialog_id"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("dialog_id is required")

        telegramClient.markAsRead(chatId)
        return mapOf("ok" to true)
    }
}
