package org.example.mcptelegram.security

import org.example.mcptelegram.AbstractIntegrationTest
import org.example.mcptelegram.persistence.repository.McpUserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DataInitializerTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mcpUserRepository: McpUserRepository

    @Test
    fun `should have admin user created on startup`() {
        val admin = mcpUserRepository.findByUsername("admin")
        assertTrue(admin.isPresent)
    }

    @Test
    fun `should have mcp user created on startup`() {
        val mcp = mcpUserRepository.findByUsername("mcp")
        assertTrue(mcp.isPresent)
    }
}
