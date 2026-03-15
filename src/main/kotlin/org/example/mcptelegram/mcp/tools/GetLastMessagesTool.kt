package org.example.mcptelegram.mcp.tools

import org.example.mcptelegram.mcp.McpToolHandler
import org.example.mcptelegram.telegram.TelegramClient
import org.springframework.stereotype.Component

@Component
class GetLastMessagesTool(
    private val telegramClient: TelegramClient
) : McpToolHandler {

    override val name = "get_last_messages"
    override val description = "Get N last messages from a dialog"
    override val inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "dialog_id" to mapOf("type" to "integer", "description" to "Telegram chat ID"),
            "count" to mapOf("type" to "integer", "description" to "Number of messages to retrieve", "default" to 10)
        ),
        "required" to listOf("dialog_id")
    )

    override suspend fun execute(params: Map<String, Any?>): Any {
        val chatId = (params["dialog_id"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("dialog_id is required")
        val count = (params["count"] as? Number)?.toInt() ?: 10

        return telegramClient.getMessages(chatId, count).map { message ->
            mapOf(
                "message_id" to message.messageId,
                "chat_id" to message.chatId,
                "sender_id" to message.senderId,
                "sender_name" to message.senderName,
                "text" to message.text,
                "date" to message.date.toString()
            )
        }
    }
}
