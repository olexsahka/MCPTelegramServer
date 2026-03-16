package org.example.mcptelegram.telegram

import it.tdlight.Init
import it.tdlight.client.APIToken
import it.tdlight.client.AuthenticationSupplier
import it.tdlight.client.ClientInteraction
import it.tdlight.client.InputParameter
import it.tdlight.client.SimpleTelegramClientFactory
import it.tdlight.client.TDLibSettings
import it.tdlight.jni.TdApi
import kotlinx.coroutines.future.await
import org.example.mcptelegram.telegram.model.ChatType
import org.example.mcptelegram.telegram.model.Dialog
import org.example.mcptelegram.telegram.model.Message
import org.example.mcptelegram.telegram.model.TelegramUpdate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Scanner  // used as fallback for local dev stdin auth
import java.util.concurrent.CompletableFuture
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Component
class TdLibTelegramClient(
    @Value("\${telegram.api-id}") private val apiId: Int,
    @Value("\${telegram.api-hash}") private val apiHash: String,
    @Value("\${telegram.phone}") private val phone: String,
    @Value("\${telegram.database-directory:./tdlib-data}") private val databaseDirectory: String,
    @Value("\${telegram.auth-code:}") private val authCode: String,
    @Value("\${telegram.auth-password:}") private val authPassword: String
) : TelegramClient {

    private val log = LoggerFactory.getLogger(TdLibTelegramClient::class.java)
    private var updateHandler: ((TelegramUpdate) -> Unit)? = null

    private lateinit var factory: SimpleTelegramClientFactory
    private lateinit var client: it.tdlight.client.SimpleTelegramClient

    @PostConstruct
    fun init() {
        Init.init()

        factory = SimpleTelegramClientFactory()

        val settings = TDLibSettings.create(APIToken(apiId, apiHash)).apply {
            val sessionPath = Paths.get(databaseDirectory)
            databaseDirectoryPath = sessionPath.resolve("data")
            downloadedFilesDirectoryPath = sessionPath.resolve("downloads")
        }

        val builder = factory.builder(settings)

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState::class.java) { update ->
            log.info("TDLib auth state: {}", update.authorizationState::class.simpleName)
            when (update.authorizationState) {
                is TdApi.AuthorizationStateReady -> log.info("TDLib: authorized successfully")
                is TdApi.AuthorizationStateLoggingOut -> log.info("TDLib: logging out")
                is TdApi.AuthorizationStateClosed -> log.info("TDLib: client closed")
                else -> {}
            }
        }

        builder.addUpdateHandler(TdApi.UpdateNewMessage::class.java) { update ->
            val msg = mapMessage(update.message)
            updateHandler?.invoke(TelegramUpdate.NewMessage(msg))
        }

        val interaction = ClientInteraction { parameter, _ ->
            when (parameter) {
                InputParameter.ASK_CODE -> {
                    if (authCode.isNotBlank()) {
                        log.info("TDLib: using auth code from environment")
                        CompletableFuture.completedFuture(authCode.trim())
                    } else {
                        // Fallback: read from stdin (local dev)
                        print("Enter Telegram code: ")
                        CompletableFuture.completedFuture(Scanner(System.`in`).nextLine().trim())
                    }
                }
                InputParameter.ASK_PASSWORD -> {
                    if (authPassword.isNotBlank()) {
                        log.info("TDLib: using 2FA password from environment")
                        CompletableFuture.completedFuture(authPassword.trim())
                    } else {
                        print("Enter 2FA password: ")
                        CompletableFuture.completedFuture(Scanner(System.`in`).nextLine().trim())
                    }
                }
                else -> CompletableFuture.completedFuture("")
            }
        }
        builder.setClientInteraction(interaction)

        val authSupplier = AuthenticationSupplier.user(phone)
        client = builder.build(authSupplier)

        log.info("TDLib client started")
    }

    @PreDestroy
    fun destroy() {
        if (::client.isInitialized) runCatching { client.close() }
        if (::factory.isInitialized) runCatching { factory.close() }
    }

    override suspend fun getDialogs(limit: Int, offset: Int): List<Dialog> {
        // GetChats returns IDs already known to TDLib cache — no blocking call needed
        val chats = client.send(TdApi.GetChats(TdApi.ChatListMain(), limit + offset)).await()
        return chats.chatIds.toList().drop(offset).mapNotNull { chatId: Long ->
            runCatching {
                client.send(TdApi.GetChat(chatId)).await().let { mapChat(it) }
            }.onFailure { log.warn("Failed to get chat $chatId", it) }.getOrNull()
        }
    }

    override suspend fun searchDialog(query: String): List<Dialog> {
        // Special case: "saved messages" or "избранное" → return self chat
        if (query.lowercase() in listOf("saved messages", "saved", "избранное", "избранн")) {
            val me = runCatching { client.send(TdApi.GetMe()).await() }.getOrNull()
            if (me != null) {
                val chat = runCatching { client.send(TdApi.GetChat(me.id)).await() }.getOrNull()
                if (chat != null) return listOf(mapChat(chat))
            }
        }
        val result = client.send(TdApi.SearchChats(query, 20)).await()
        return result.chatIds.toList().mapNotNull { chatId: Long ->
            runCatching {
                client.send(TdApi.GetChat(chatId)).await().let { mapChat(it) }
            }.onFailure { log.warn("Failed to get chat $chatId", it) }.getOrNull()
        }
    }

    override suspend fun getMessages(chatId: Long, limit: Int, fromMessageId: Long): List<Message> {
        val history = client.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)).await()
        return history.messages.map { mapMessage(it) }
    }

    override suspend fun getMessage(chatId: Long, messageId: Long): Message? {
        return runCatching {
            client.send(TdApi.GetMessage(chatId, messageId)).await().let { mapMessage(it) }
        }.onFailure { log.warn("Failed to get message $messageId in chat $chatId", it) }.getOrNull()
    }

    override suspend fun getUnreadMessages(limit: Int): List<Message> {
        val dialogs = getDialogs()
        return dialogs
            .filter { it.unreadCount > 0 }
            .flatMap { dialog ->
                runCatching {
                    getMessages(dialog.chatId, dialog.unreadCount.coerceAtLeast(1))
                }.onFailure { log.warn("Failed to get messages for chat ${dialog.chatId}", it) }
                    .getOrElse { emptyList() }
            }
            .take(limit)
    }

    override suspend fun sendMessage(chatId: Long, text: String, replyToMessageId: Long?): Message {
        val req = TdApi.SendMessage().apply {
            this.chatId = chatId
            inputMessageContent = TdApi.InputMessageText().apply {
                this.text = TdApi.FormattedText(text, emptyArray())
            }
            if (replyToMessageId != null) {
                replyTo = TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0)
            }
        }
        val sent = client.sendMessage(req, true).await()
        return mapMessage(sent)
    }

    override suspend fun markAsRead(chatId: Long) {
        val history = client.send(TdApi.GetChatHistory(chatId, 0, 0, 1, false)).await()
        val messageIds = history.messages.map { it.id }.toLongArray()
        if (messageIds.isNotEmpty()) {
            client.send(TdApi.ViewMessages(chatId, messageIds, null, true)).await()
        }
    }

    override fun setUpdateHandler(handler: (TelegramUpdate) -> Unit) {
        this.updateHandler = handler
    }

    private fun mapChat(chat: TdApi.Chat): Dialog {
        val type = when (val t = chat.type) {
            is TdApi.ChatTypePrivate -> ChatType.PRIVATE
            is TdApi.ChatTypeBasicGroup -> ChatType.GROUP
            is TdApi.ChatTypeSupergroup -> if (t.isChannel) ChatType.CHANNEL else ChatType.SUPERGROUP
            else -> ChatType.UNKNOWN
        }
        return Dialog(
            chatId = chat.id,
            type = type,
            title = chat.title,
            unreadCount = chat.unreadCount,
            lastMessage = chat.lastMessage?.let { mapMessage(it) }
        )
    }

    private fun mapMessage(msg: TdApi.Message): Message {
        val text = when (val c = msg.content) {
            is TdApi.MessageText -> c.text.text
            else -> "[${c::class.simpleName}]"
        }
        val senderId = when (val s = msg.senderId) {
            is TdApi.MessageSenderUser -> s.userId
            is TdApi.MessageSenderChat -> s.chatId
            else -> null
        }
        val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(msg.date.toLong()), ZoneId.systemDefault())
        return Message(messageId = msg.id, chatId = msg.chatId, senderId = senderId, text = text, date = date)
    }
}
