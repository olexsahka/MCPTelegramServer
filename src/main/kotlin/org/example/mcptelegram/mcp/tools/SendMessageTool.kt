package org.example.mcptelegram.mcp.tools

import org.example.mcptelegram.mcp.McpToolHandler
import org.example.mcptelegram.telegram.TelegramClient
import org.springframework.stereotype.Component

@Component
class SendMessageTool(
    private val telegramClient: TelegramClient
) : McpToolHandler {

    override val name = "send_message"
    override val description = "Send a message to a Telegram dialog"
    override val inputSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "dialog_id" to mapOf("type" to "integer", "description" to "Telegram chat ID"),
            "text" to mapOf("type" to "string", "description" to "Message text")
        ),
        "required" to listOf("dialog_id", "text")
    )

    override suspend fun execute(params: Map<String, Any?>): Any {
        val chatId = (params["dialog_id"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("dialog_id is required")
        val text = params["text"] as? String
            ?: throw IllegalArgumentException("text is required")

        val message = telegramClient.sendMessage(chatId, text)
        return mapOf(
            "message_id" to message.messageId,
            "chat_id" to message.chatId,
            "text" to message.text,
            "date" to message.date.toString()
        )
    }
}
