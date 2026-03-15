package org.example.mcptelegram.persistence.repository

import org.example.mcptelegram.persistence.entity.DialogEntity
import org.example.mcptelegram.persistence.entity.MessageEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface MessageRepository : JpaRepository<MessageEntity, Long> {
    fun findByDialogOrderBySentAtDesc(dialog: DialogEntity, pageable: Pageable): List<MessageEntity>
    fun findByTelegramMessageId(telegramMessageId: Long): Optional<MessageEntity>
}
