package org.example.mcptelegram.telegram

import org.example.mcptelegram.telegram.model.Dialog
import org.example.mcptelegram.telegram.model.Message
import org.example.mcptelegram.telegram.model.TelegramUpdate

interface TelegramClient {
    suspend fun getDialogs(limit: Int = 100): List<Dialog>
    suspend fun searchDialog(query: String): List<Dialog>
    suspend fun getMessages(chatId: Long, limit: Int): List<Message>
    suspend fun getUnreadMessages(): List<Message>
    suspend fun sendMessage(chatId: Long, text: String): Message
    fun setUpdateHandler(handler: (TelegramUpdate) -> Unit)
}
