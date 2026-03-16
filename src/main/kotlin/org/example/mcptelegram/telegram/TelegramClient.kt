package org.example.mcptelegram.telegram

import org.example.mcptelegram.telegram.model.Dialog
import org.example.mcptelegram.telegram.model.Message
import org.example.mcptelegram.telegram.model.TelegramUpdate

interface TelegramClient {
    suspend fun getDialogs(limit: Int = 100, offset: Int = 0): List<Dialog>
    suspend fun searchDialog(query: String): List<Dialog>
    suspend fun getMessages(chatId: Long, limit: Int, fromMessageId: Long = 0): List<Message>
    suspend fun getMessage(chatId: Long, messageId: Long): Message?
    suspend fun getUnreadMessages(limit: Int = 50): List<Message>
    suspend fun sendMessage(chatId: Long, text: String, replyToMessageId: Long? = null): Message
    suspend fun markAsRead(chatId: Long)
    fun setUpdateHandler(handler: (TelegramUpdate) -> Unit)
}
