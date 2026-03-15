package org.example.mcptelegram.security

import org.example.mcptelegram.persistence.entity.McpUserEntity
import org.example.mcptelegram.persistence.repository.McpUserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class McpUserService(
    private val mcpUserRepository: McpUserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun createUser(username: String, password: String): McpUserEntity {
        val hash = passwordEncoder.encode(password)
        return mcpUserRepository.save(McpUserEntity(username = username, passwordHash = hash))
    }

    fun authenticate(username: String, password: String): Boolean {
        val user = mcpUserRepository.findByUsername(username).orElse(null) ?: return false
        return passwordEncoder.matches(password, user.passwordHash)
    }
}
