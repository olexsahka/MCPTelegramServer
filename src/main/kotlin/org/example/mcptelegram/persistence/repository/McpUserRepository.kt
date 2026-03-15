package org.example.mcptelegram.persistence.repository

import org.example.mcptelegram.persistence.entity.McpUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface McpUserRepository : JpaRepository<McpUserEntity, Long> {
    fun findByUsername(username: String): Optional<McpUserEntity>
}
