package org.example.mcptelegram.mcp.tools

import org.example.mcptelegram.mcp.McpToolHandler
import org.example.mcptelegram.telegram.TelegramClient
import org.springframework.stereotype.Component

@Component
class GetDialogsTool(
    private val telegramClient: TelegramClient
) : McpToolHandler {

    override val name = "get_dialogs"
    override val description = "Get list of Telegram dialogs with metadata"
    override val inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "limit" to mapOf("type" to "integer", "description" to "Max number of dialogs to return", "default" to 100),
            "offset" to mapOf("type" to "integer", "description" to "Number of dialogs to skip", "default" to 0)
        ),
        "required" to emptyList<String>()
    )

    override suspend fun execute(params: Map<String, Any?>): Any {
        val limit = (params["limit"] as? Number)?.toInt() ?: 100
        val offset = (params["offset"] as? Number)?.toInt() ?: 0
        return telegramClient.getDialogs(limit, offset).map { dialog ->
            mapOf(
                "chat_id" to dialog.chatId,
                "type" to dialog.type.name,
                "title" to dialog.title,
                "unread_count" to dialog.unreadCount
            )
        }
    }
}
