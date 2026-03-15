package org.example.mcptelegram.mcp

import kotlinx.coroutines.runBlocking
import org.example.mcptelegram.mcp.model.McpError
import org.example.mcptelegram.mcp.model.McpRequest
import org.example.mcptelegram.mcp.model.McpResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mcp")
class McpController(
    private val toolRegistry: McpToolRegistry
) {

    @PostMapping
    fun handle(@RequestBody request: McpRequest): ResponseEntity<McpResponse> {
        return when (request.method) {
            "tools/list" -> {
                val tools = toolRegistry.listTools()
                ResponseEntity.ok(McpResponse(result = mapOf("tools" to tools), id = request.id))
            }
            "tools/call" -> {
                val toolName = (request.params?.get("name") as? String)
                    ?: return ResponseEntity.ok(McpResponse(
                        error = McpError(-32602, "Missing tool name"),
                        id = request.id
                    ))

                val tool = toolRegistry.getTool(toolName)
                    ?: return ResponseEntity.ok(McpResponse(
                        error = McpError(-32601, "Tool not found: $toolName"),
                        id = request.id
                    ))

                try {
                    @Suppress("UNCHECKED_CAST")
                    val toolParams = (request.params?.get("arguments") as? Map<String, Any?>) ?: emptyMap()
                    val result = runBlocking { tool.execute(toolParams) }
                    ResponseEntity.ok(McpResponse(result = result, id = request.id))
                } catch (e: Exception) {
                    ResponseEntity.ok(McpResponse(
                        error = McpError(-32603, e.message ?: "Internal error"),
                        id = request.id
                    ))
                }
            }
            else -> ResponseEntity.ok(McpResponse(
                error = McpError(-32601, "Method not found: ${request.method}"),
                id = request.id
            ))
        }
    }
}
