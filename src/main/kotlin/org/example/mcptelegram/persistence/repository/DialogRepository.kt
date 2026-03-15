package org.example.mcptelegram.persistence.repository

import org.example.mcptelegram.persistence.entity.DialogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DialogRepository : JpaRepository<DialogEntity, Long> {
    fun findByTelegramChatId(telegramChatId: Long): Optional<DialogEntity>
    fun findAllByOrderByLastMessageAtDesc(): List<DialogEntity>
}
