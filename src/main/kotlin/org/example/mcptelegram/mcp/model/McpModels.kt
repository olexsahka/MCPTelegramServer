package org.example.mcptelegram.mcp.model

import com.fasterxml.jackson.annotation.JsonInclude

data class McpRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Map<String, Any?>? = null,
    val id: Any? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class McpResponse(
    val jsonrpc: String = "2.0",
    val result: Any? = null,
    val error: McpError? = null,
    val id: Any? = null
)

data class McpError(
    val code: Int,
    val message: String
)

data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any>
)
