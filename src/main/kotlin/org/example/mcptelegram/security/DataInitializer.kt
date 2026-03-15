package org.example.mcptelegram.security

import org.example.mcptelegram.persistence.repository.McpUserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val mcpUserRepository: McpUserRepository,
    private val mcpUserService: McpUserService,
    @Value("\${mcp.auth.username:mcp}") private val mcpUsername: String,
    @Value("\${mcp.auth.password}") private val mcpPassword: String,
    @Value("\${mcp.admin.username:admin}") private val adminUsername: String,
    @Value("\${mcp.admin.password:admin}") private val adminPassword: String
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        if (mcpUserRepository.count() == 0L) {
            mcpUserService.createUser(adminUsername, adminPassword)
            logger.info("Created default admin user: {}", adminUsername)
            mcpUserService.createUser(mcpUsername, mcpPassword)
            logger.info("Created MCP user: {}", mcpUsername)
        }
    }
}
