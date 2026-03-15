package org.example.mcptelegram.mcp

interface McpToolHandler {
    val name: String
    val description: String
    val inputSchema: Map<String, Any>
    suspend fun execute(params: Map<String, Any?>): Any
}
