package org.example.mcptelegram.messaging

import org.example.mcptelegram.messaging.dto.DialogDto
import org.example.mcptelegram.messaging.dto.MessageDto
import org.example.mcptelegram.persistence.entity.DialogEntity
import org.example.mcptelegram.persistence.entity.MessageEntity
import org.example.mcptelegram.persistence.repository.DialogRepository
import org.example.mcptelegram.persistence.repository.MessageRepository
import org.example.mcptelegram.persistence.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TelegramPersistenceService(
    private val userRepository: UserRepository,
    private val dialogRepository: DialogRepository,
    private val messageRepository: MessageRepository
) {

    @Transactional
    fun saveMessage(messageDto: MessageDto) {
        val dialog = dialogRepository.findByTelegramChatId(messageDto.chatId)
            .orElseGet {
                dialogRepository.save(DialogEntity(
                    telegramChatId = messageDto.chatId,
                    type = "UNKNOWN",
                    title = "Chat ${messageDto.chatId}",
                    lastMessageAt = messageDto.sentAt
                ))
            }

        dialog.lastMessageAt = messageDto.sentAt
        dialog.updatedAt = LocalDateTime.now()
        dialogRepository.save(dialog)

        if (messageRepository.findByTelegramMessageId(messageDto.messageId).isEmpty) {
            messageRepository.save(MessageEntity(
                dialog = dialog,
                text = messageDto.text,
                telegramMessageId = messageDto.messageId,
                sentAt = messageDto.sentAt
            ))
        }
    }

    @Transactional
    fun updateDialog(dialogDto: DialogDto) {
        val existing = dialogRepository.findByTelegramChatId(dialogDto.chatId)
        if (existing.isPresent) {
            val dialog = existing.get()
            dialog.updatedAt = LocalDateTime.now()
            dialogDto.lastMessageAt?.let { dialog.lastMessageAt = it }
            dialogRepository.save(dialog)
        } else {
            dialogRepository.save(DialogEntity(
                telegramChatId = dialogDto.chatId,
                type = dialogDto.type,
                title = dialogDto.title,
                lastMessageAt = dialogDto.lastMessageAt
            ))
        }
    }
}
