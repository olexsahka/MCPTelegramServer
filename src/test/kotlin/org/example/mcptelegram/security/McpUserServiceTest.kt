package org.example.mcptelegram.security

import org.example.mcptelegram.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class McpUserServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mcpUserService: McpUserService

    @Test
    fun `should create user and authenticate`() {
        mcpUserService.createUser("testuser_service", "password123")

        assertTrue(mcpUserService.authenticate("testuser_service", "password123"))
        assertFalse(mcpUserService.authenticate("testuser_service", "wrongpassword"))
    }

    @Test
    fun `should return false for non-existing user`() {
        assertFalse(mcpUserService.authenticate("nonexistent_user_xyz", "anypassword"))
    }
}
