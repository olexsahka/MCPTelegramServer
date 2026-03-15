package org.example.mcptelegram.mcp.tools

import org.example.mcptelegram.mcp.McpToolHandler
import org.example.mcptelegram.telegram.TelegramClient
import org.springframework.stereotype.Component

@Component
class GetNewMessagesTool(
    private val telegramClient: TelegramClient
) : McpToolHandler {

    override val name = "get_unread_messages"
    override val description = "Get all unread messages across all dialogs"
    override val inputSchema = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>(),
        "required" to emptyList<String>()
    )

    override suspend fun execute(params: Map<String, Any?>): Any {
        return telegramClient.getUnreadMessages().map { message ->
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
