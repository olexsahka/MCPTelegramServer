package org.example.mcptelegram.mcp.tools

import org.example.mcptelegram.mcp.McpToolHandler
import org.example.mcptelegram.telegram.TelegramClient
import org.springframework.stereotype.Component

@Component
class SearchDialogTool(
    private val telegramClient: TelegramClient
) : McpToolHandler {

    override val name = "search_dialog"
    override val description = "Search Telegram dialogs by name"
    override val inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "query" to mapOf("type" to "string", "description" to "Search query (dialog name)")
        ),
        "required" to listOf("query")
    )

    override suspend fun execute(params: Map<String, Any?>): Any {
        val query = params["query"] as? String
            ?: throw IllegalArgumentException("query is required")

        return telegramClient.searchDialog(query).map { dialog ->
            mapOf(
                "chat_id" to dialog.chatId,
                "type" to dialog.type.name,
                "title" to dialog.title,
                "unread_count" to dialog.unreadCount
            )
        }
    }
}
