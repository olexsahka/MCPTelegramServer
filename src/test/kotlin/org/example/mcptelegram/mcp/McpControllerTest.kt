package org.example.mcptelegram.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.mcptelegram.mcp.model.McpRequest
import org.example.mcptelegram.mcp.model.McpToolDefinition
import org.example.mcptelegram.security.McpUserDetailsService
import org.example.mcptelegram.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(McpController::class)
@Import(SecurityConfig::class)
class McpControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var toolRegistry: McpToolRegistry

    @MockBean
    lateinit var mcpUserDetailsService: McpUserDetailsService

    @Test
    @WithMockUser
    fun `should return tools list on tools_list request`() {
        whenever(toolRegistry.listTools()).thenReturn(listOf(
            McpToolDefinition("get_dialogs", "Get dialogs", emptyMap())
        ))

        val request = McpRequest(method = "tools/list", id = 1)

        mockMvc.post("/mcp") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.result.tools[0].name") { value("get_dialogs") }
        }
    }

    @Test
    @WithMockUser
    fun `should return error for unknown tool`() {
        whenever(toolRegistry.getTool("unknown_tool")).thenReturn(null)

        val request = McpRequest(method = "tools/call", params = mapOf("name" to "unknown_tool"), id = 2)

        mockMvc.post("/mcp") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.error.code") { value(-32601) }
        }
    }

    @Test
    fun `should return 401 without authentication`() {
        val request = McpRequest(method = "tools/list", id = 3)

        mockMvc.post("/mcp") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
