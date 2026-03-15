package org.example.mcptelegram.persistence

import org.example.mcptelegram.AbstractIntegrationTest
import org.example.mcptelegram.persistence.entity.McpUserEntity
import org.example.mcptelegram.persistence.repository.McpUserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class McpUserRepositoryTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mcpUserRepository: McpUserRepository

    @Test
    fun `should save and find mcp user by username`() {
        val user = McpUserEntity(username = "admin_test_unique", passwordHash = "\$2a\$10\$hash")
        mcpUserRepository.save(user)

        val found = mcpUserRepository.findByUsername("admin_test_unique")
        assertTrue(found.isPresent)
        assertEquals("admin_test_unique", found.get().username)
    }

    @Test
    fun `should return empty for non-existing username`() {
        val found = mcpUserRepository.findByUsername("nonexistent_xyz")
        assertFalse(found.isPresent)
    }
}
