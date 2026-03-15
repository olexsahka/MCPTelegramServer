package org.example.mcptelegram.admin

import org.example.mcptelegram.persistence.repository.DialogRepository
import org.example.mcptelegram.persistence.repository.MessageRepository
import org.example.mcptelegram.security.McpUserDetailsService
import org.example.mcptelegram.security.SecurityConfig
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(AdminController::class)
@Import(SecurityConfig::class)
class AdminControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var dialogRepository: DialogRepository

    @MockBean
    lateinit var messageRepository: MessageRepository

    @MockBean
    lateinit var mcpUserDetailsService: McpUserDetailsService

    @Test
    @WithMockUser
    fun `should return dashboard page`() {
        whenever(dialogRepository.count()).thenReturn(5L)
        whenever(messageRepository.count()).thenReturn(100L)

        mockMvc.get("/admin").andExpect {
            status { isOk() }
            content { string(Matchers.containsString("Dashboard")) }
        }
    }

    @Test
    @WithMockUser
    fun `should return dialogs page`() {
        whenever(dialogRepository.findAllByOrderByLastMessageAtDesc()).thenReturn(emptyList())

        mockMvc.get("/admin/dialogs").andExpect {
            status { isOk() }
            content { string(Matchers.containsString("Dialogs")) }
        }
    }

    @Test
    fun `should return 401 without auth`() {
        mockMvc.get("/admin").andExpect {
            status { isUnauthorized() }
        }
    }
}
