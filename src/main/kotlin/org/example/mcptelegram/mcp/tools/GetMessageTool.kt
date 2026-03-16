package org.example.mcptelegram.mcp.tools

import org.example.mcptelegram.mcp.McpToolHandler
import org.example.mcptelegram.telegram.TelegramClient
import org.springframework.stereotype.Component

@Component
class GetMessageTool(
    private val telegramClient: TelegramClient
) : McpToolHandler {

    override val name = "get_message"
    override val description = "Get a specific message by ID from a dialog"
    override val inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "dialog_id" to mapOf("type" to "number", "description" to "Telegram chat ID"),
            "message_id" to mapOf("type" to "number", "description" to "Telegram message ID")
        ),
        "required" to listOf("dialog_id", "message_id")
    )

    override suspend fun execute(params: Map<String, Any?>): Any {
        val chatId = (params["dialog_id"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("dialog_id is required")
        val messageId = (params["message_id"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("message_id is required")

        val message = telegramClient.getMessage(chatId, messageId)
            ?: return mapOf("error" to "Message not found")

        return mapOf(
            "message_id" to message.messageId,
            "chat_id" to message.chatId,
            "sender_id" to message.senderId,
            "sender_name" to message.senderName,
            "text" to message.text,
            "date" to message.date.toString()
        )
    }
}
