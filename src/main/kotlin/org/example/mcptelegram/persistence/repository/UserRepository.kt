package org.example.mcptelegram.persistence.repository

import org.example.mcptelegram.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByTelegramId(telegramId: Long): Optional<UserEntity>
}
