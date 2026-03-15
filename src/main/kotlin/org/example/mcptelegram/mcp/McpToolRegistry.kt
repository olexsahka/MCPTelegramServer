package org.example.mcptelegram.mcp

import org.example.mcptelegram.mcp.model.McpToolDefinition
import org.springframework.stereotype.Component

@Component
class McpToolRegistry(handlers: List<McpToolHandler>) {

    private val tools: Map<String, McpToolHandler> = handlers.associateBy { it.name }

    fun getTool(name: String): McpToolHandler? = tools[name]

    fun listTools(): List<McpToolDefinition> = tools.values.map { handler ->
        McpToolDefinition(
            name = handler.name,
            description = handler.description,
            inputSchema = handler.inputSchema
        )
    }
}
