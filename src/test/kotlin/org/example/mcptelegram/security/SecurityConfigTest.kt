package org.example.mcptelegram.security

import org.example.mcptelegram.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class SecurityConfigTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should return 401 for admin without auth`() {
        mockMvc.get("/admin").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `should return 401 for mcp without auth`() {
        mockMvc.post("/mcp") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"jsonrpc":"2.0","method":"tools/list","id":1}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `should return 200 for actuator health without auth`() {
        mockMvc.get("/actuator/health").andExpect { status { isOk() } }
    }
}
